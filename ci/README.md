# CI/CD Scripts

mkvm_vbox.sh - create a "worker" CentOS 9 VM on VirtualBox.

Currently tailored for Mac x86 and brew environment, but should be suitable for Linux and Win x86


Ansible
-------------------

Playbook to setup worker VMs and build the code. Validated with CentOS 9.

play.sh execute the playbook for all build workers.

inventory.txt lists worker VMs with their associated roles.

## roles

### zephyr

Install Zephyr src code and SDK under `zephyr` user home dir.

Other users are expected to register Zephyr SDK once with
```
~zephyr/zephyr_sdk_setup.sh
```

Then every time activate the Zephyr west env with
```
source ~zephyr/zephyr_venv_activate
```
