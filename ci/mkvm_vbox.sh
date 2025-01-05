#!/bin/bash
#
# Make a "worker" VM using VirtualBox to executed CI locally
#
# Download Cloud CentOS 9 image and provision "admin" user
# authenticated using current ~/.ssh/id_rsa.pub
#
# See usage()
#
set -o errexit

ADMIN="admin"
KEY_FILE=~/.ssh/id_rsa.pub

BASEFOLDER="/Volumes/B2/VirtualBox"
IMG_URL=https://cloud.centos.org/centos/9-stream/x86_64/images/CentOS-Stream-GenericCloud-9-latest.x86_64.qcow2

IMG=~/Downloads/${IMG_URL##*/} 

usage() {
	cat <<EOF
This script creates or deletes CentOS Virtual Box VM
Usage:
$0 [-d] [<name>]

-d - delete the VM

EOF

	check_virtual_box
}

check_virtual_box() {
	if ! VBoxManage >/dev/null 2>&1; then
		cat <<EOF
You don't have VirtualBox software installed

cd ~/Downloads
curl -O https://download.virtualbox.org/virtualbox/6.1.34/VirtualBox-6.1.34-150636-OSX.dmg
open ./VirtualBox-6.1.34-150636-OSX.dmg

click on VirtualBox.pkg and begin installation

At the end it'll ask to go to Security&Privacy where you need to choose "App store and other developers" and select "Oracle Inc"

Then restart your Mac
EOF
		exit 1
	fi
}

delete_vm() {
	check_virtual_box
	sed -i -e '/^\[localhost\]:2222/d' ~/.ssh/known_hosts
	# disregard other errors
	set +o errexit 
	VBoxManage controlvm "$VMNAME" poweroff --type headless
	sleep 5
	VBoxManage unregistervm "$VMNAME" --delete
	VBoxManage closemedium disk "$IMG"
	rm -rf "$VMDIR"
	echo "Deleted $VMNAME"
}

download_image() {
	# download cloud image if we need to
	if [ ! -f "$IMG" ]; then
		echo "Downloading OS image..."
		curl --output "$IMG" "$IMG_URL"
		chmod -w "$IMG"
	fi
}

create_vm() {
	mac="A0:CE:C8:DF:24:01" # random, but predefined
	check_virtual_box
	
	download_image

	if [ ! -f $KEY_FILE ]; then
		echo "You must create an ssh key pair to be able to login to VM"
		ssh-keygen
	fi

	# create VM dir
	mkdir -p "$VMDIR"
	cd "$VMDIR"

	#
	# compose cloud-init files as ISO image
	#
	mkdir -p cloud-config
	cat >cloud-config/meta-data <<EOF
instance-id: $VMNAME
local-hostname: $VMNAME
EOF
	# FYI, more things to do with cloud-init: https://cloudinit.readthedocs.io/en/latest/topics/examples.html
    cat >cloud-config/user-data <<EOF
#cloud-config
users:
- name: ${ADMIN}
  sudo: ALL=(ALL) NOPASSWD:ALL
  groups: users
  primary_group: adm
  lock_passwd: false
  ssh_authorized_keys:
  - $(cat $KEY_FILE)
runcmd:
- systemctl enable --now cockpit.socket
power_state:
  mode: poweroff
EOF
	hdiutil makehybrid -o cloud-config.iso -ov -hfs -joliet -iso -default-volume-name cidata cloud-config/

	#
	# create the VM
	#
	VBoxManage createvm --name "$VMNAME" --ostype "RedHat_64" --register --basefolder "$BASEFOLDER"
	VBoxManage modifyvm "$VMNAME" --ioapic on
	VBoxManage modifyvm "$VMNAME" --memory 8192 --vram 12

	# NAT through host, but redirect localhost:2222 -> VM:22
	VBoxManage modifyvm "$VMNAME" --nic1 nat
	VBoxManage modifyvm "$VMNAME" --macaddress1 "$(echo $mac | sed -e 's|:||g')"   # remove :
	VBoxManage modifyvm "$VMNAME" --natpf1 SSH2VM,tcp,127.0.0.1,2222,10.0.2.15,22 

	# NAT through host, but redirect cockpit:9090 -> VM:9090
	VBoxManage modifyvm "$VMNAME" --natpf1 COCKPIT,tcp,127.0.0.1,9090,10.0.2.15,9090
	
	# hdd image + cloud-init.iso
	VBoxManage storagectl "$VMNAME" --name "SATA Controller" --add sata --controller IntelAhci
	VBoxManage clonehd disk "$IMG" "$VMDIR/vmdisk.vmdk" --format VMDK

	VBoxManage storageattach "$VMNAME" --storagectl "SATA Controller" --port 0 --device 0 --type hdd --medium "$VMDIR/vmdisk.vmdk"
	VBoxManage storageattach "$VMNAME" --storagectl "SATA Controller" --port 1 --device 0 --type dvddrive --medium "$VMDIR/cloud-config.iso"

	#VBoxManage startvm "$VMNAME" --type headless
	VBoxManage startvm "$VMNAME"

	echo "$VMNAME is up and performing initial configuration. Waiting for complettion"
	vm_match="VirtualBoxVM --comment $VMNAME"
	while pgrep -U $USER -f "$vm_match" >/dev/null; do
		echo -n "."
		sleep 10
	done
	cat <<EOF
-----------------------------------------------------------------------------
VM is configured and ready to start. To start the VM run

   VBoxManage startvm "$VMNAME" [--type headless]

Give it a minute to boot and then login with SSH

   ssh -p 2222 admin@localhost

It also starts a cockpit Web UI on https://localhost:9090

EOF
}


#
# Main
#
act="create_vm"

while getopts "hd" arg; do
	case $arg in
		h)
			usage
			exit 0
			;;
		d)
			act="delete_vm"
			;;
	esac
done

shift $((OPTIND-1))

VMNAME=${1:-"worker"}
VMDIR=$BASEFOLDER/$VMNAME

$act

