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

	check_qemu
}

check_qemu() {
	if ! qemu-system-x86_64 --version >/dev/null 2>&1; then
		cat <<EOF
You don't have QEMU software installed

Install homebrew: https://docs.brew.sh/Installation
and QEMU package with

brew install qemu

Check if the installation was successful with

qemu-system-x86_64 --version
EOF
		exit 1
	fi
}

delete_vm() {
	check_qemu
	sed -i -e '/^\[localhost\]:2222/d' ~/.ssh/known_hosts
	# disregard other errors
	set +o errexit
	# TODO stop VM
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

start_vm() {
	cd "$VMDIR"
	qemu-system-x86_64  \
		-cpu host -m 8192 \
		-accel hvf \
		-nographic \
		-snapshot \
		-netdev id=net00,type=user,hostfwd=tcp::2222-:22 \
		-device virtio-net-pci,netdev=net00 \
		-drive if=virtio,format=qcow2,file=disk.qcow2 \
		$@
}

stop_vm() {
	cd "$VMDIR"
	echo "Stop Not implemented"
}

create_vm() {	
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
power_state:
  mode: poweroff
EOF
	hdiutil makehybrid -o cloud-config.iso -ov -hfs -joliet -iso -default-volume-name cidata cloud-config/

	#
	# create the VM
	#
	
	cp -f "$IMG" disk.qcow2
	chmod +w disk.qcow2
	qemu-img resize disk.qcow2 20G

	exec run_vm -cdrom cloud-config.iso &
	pid=$!
	wait $pid
	# TODO better message
	echo "vm created"
}


#
# Main
#

check_qemu

act="start_vm"

while getopts "hd" arg; do
	case $arg in
		h)
			usage
			exit 0
			;;
		t)
			act="stop_vm"
			;;
		c)
			act="create_vm"
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

