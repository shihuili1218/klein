unset SSH_AUTH_SOCK
lein run test --time-limit 6000 --concurrency 5 --test-count 5 --username root --password 123456 $@
