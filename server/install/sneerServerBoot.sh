cd /root/simploy
git pull
rm *.class
javac Simploy.java

cd /root/server
java -cp /root/simploy/ Simploy 44321 senhamalucaqq&

cd /root/snapi
java -cp /root/simploy/ Simploy 44322 senhamalucaqq&

cd /root/android.main
java -cp /root/simploy/ Simploy 44323 senhamalucaqq&

cd /root/android.chat
java -cp /root/simploy/ Simploy 44324 senhamalucaqq&

