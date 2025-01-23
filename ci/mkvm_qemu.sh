#!/bin/bash
#
# Make a "worker" VM using QEMU on Mac to execute CI locally
#
# It downloads Cloud CentOS 9 image, creates VM and provisions "admin"
# user authenticated using current ~/.ssh/id_rsa.pub
# No password access given
#
# For more see usage()
#
set -o errexit

ADMIN="admin"
KEY_FILE=~/.ssh/id_rsa.pub

BASEFOLDER="/Volumes/B2/VirtualBox"
IMG_URL=https://cloud.centos.org/centos/9-stream/x86_64/images/CentOS-Stream-GenericCloud-9-latest.x86_64.qcow2

IMG=~/Downloads/${IMG_URL##*/} 

die() {
	echo $@ >&2
	exit 1
}

usage() {
	cat <<EOF
This script starts, stops, creates or deletes CentOS VM
Usage:
$0 [-t|-c|-d] [<name>]

-c - create VM
-t - terminate VM
-d - delete the VM

by default start VM

EOF
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

start_vm() {
	cd "$VMDIR"
	if [ -f pid ]; then
		die "$VMNAME is already running"
	fi
	qemu-system-x86_64  \
		-cpu host -m 8192 \
		-accel hvf \
		-nographic \
		-snapshot \
		-netdev id=net0,type=user,hostfwd=tcp:127.0.0.1:2222-:22 \
		-device virtio-net-pci,netdev=net0 \
		-drive if=virtio,format=qcow2,file=disk.qcow2 \
		-pidfile pid \
		-qmp unix:qmp.sock,server,nowait \
		$@
}

stop_vm() {
	cd "$VMDIR"
	if [ -f pid ]; then
		echo "Shutting down $VMNAME..."
		echo '{"execute": "qmp_capabilities"}{"execute": "system_powerdown"}' | nc -U qmp.sock
		while [ -f pid ]; do
			sleep 1
		done
		echo "$VMNAME stopped"
	else
		echo "$VMNAME is not running"
	fi
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
	if [ -d "$VMDIR" ]; then
		die "VM directory $VMDIR already exists"
	fi
	
	download_image

	if [ ! -f $KEY_FILE ]; then
		echo "Creating ssh key pair to be able to login to VM"
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

	# create and resize VM disk
   	cp -f "$IMG" disk.qcow2
	chmod +w disk.qcow2
	qemu-img resize disk.qcow2 20G

	# pass the cloud config and wait until VM shuts down
	start_vm -cdrom cloud-config.iso
	cat <<EOF
-----------------------------------------------------------------------------
VM is configured and ready to start. To start the VM do

   $0 "$VMNAME"

Give it a minute to boot and then login from another window with SSH

   ssh -p 2222 admin@localhost
EOF
}

delete_vm() {
    read -p "Do you wish to delete VM $VMNAME? " yn
    case $yn in
        [Yy]* ) break;;
        * ) exit 0;;
    esac
	sed -i -e '/^\[localhost\]:2222/d' ~/.ssh/known_hosts
	stop_vm
	rm -rf "$VMDIR"
	echo "Deleted $VMNAME"
}

#
# Main
#

check_qemu

act="start_vm"

while getopts "htcd" arg; do
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

VMNAME="${1:-worker}"
VMDIR="$BASEFOLDER/$VMNAME"

$act
