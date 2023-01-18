unset SSH_AUTH_SOCK
lein run test --username {user_name} --nodes-file  ./nodes  --ssh-private-key {ssh_key_path} $@
