

# for dev version

```sh
mvn package

sudo /etc/init.d/jenkins stop

cd /var/lib/jenkins/plugins/
sudo rm -rf composer-security-checker/ composer-security-checker.hpi composer-security-checker.hpi.pinned
sudo cp composerSecurityChecker.hpi to /var/lib/jenkins/plugins/
sudo touch /var/lib/jenkins/plugins/composer-security-checker.hpi.pinned


sudo /etc/init.d/jenkins start
```


# for make new release version on jenkins repo

```sh
mvn release:prepare release:perform -Dusername=... -Dpassword=...
```

