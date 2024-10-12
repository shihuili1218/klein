unset SSH_AUTH_SOCK
lein run test --time-limit 600 --concurrency 5 --test-count 2 --username root --password 123456 $@
