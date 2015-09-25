#include <Servo.h>
#define PASSWORD "1234"

Servo myservo;
int pos = 0;
String incoming="";
int startflag=0;
int flag=0;
int locked=1; //initally in locked position
String action;
int startPos = 0;
int zero=0;
char letters[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; 



void setup() {
  pinMode(13,OUTPUT); //LED
    myservo.attach(9); //servo
    Serial.begin(9600); //bluetooth
}


String decrypt(String pass){
  char charKey = pass.charAt(pass.length()-1);
  for(int i=0;i<pass.length();i++) {
    for(int n=0;n<26;n++) {
      if(pass.charAt(i)==letters[n]){
        pass.setCharAt(i,String(n).charAt(0));
      }
    }
  }
  
  for(int i=0;i<pass.length()-1;i++) {
    
    pass.setCharAt(i,String(pass.charAt(i)-pass.charAt(pass.length()-1)).charAt(0));
  }

  return pass;
}

void loop() {
 if(myservo.attached() && startflag == 0) { //because servo is sometimes connected AFTER arduino is powered
 myservo.write(1); //make sure servo is in correct position
 startflag = 1;
 }
 
 if (Serial.available()>0) { //incoming messgae over bluetooth
   incoming = decrypt(Serial.readStringUntil('&'));//message of form "<PASSWORD>&<ACTION>"
   action = Serial.readString(); //value of "L" or "U" for lock and unlock
   flag = 1;
 }
 
 if (incoming==PASSWORD && flag ==1){
  //blink led
  digitalWrite(13,HIGH);
  delay(500);
  digitalWrite(13,LOW);
  delay(500);

  //unlock
  if(action == "U" && locked ==1) {
    //blink led
    digitalWrite(13,HIGH);
    delay(100);
    digitalWrite(13,LOW);
    //turn servo
   for(pos= 1; pos<120;pos+=1) {
    myservo.write(pos);
   delay(30); 
   }
   Serial.write("1");//send successful unlock to Android device
   locked =0;
  }

  //lock
  else if (action == "L" && locked == 0) {
    //blink led
    digitalWrite(13,HIGH);
    delay(500);
    digitalWrite(13,LOW);

    //turn servo
   for(pos=120;pos>1;pos-=1) {
    myservo.write(pos);
    delay(30);
   }
   Serial.write("2"); //send succesful lock
   locked =1;
  }
  action="";
  incoming="";
  flag=0;
 }
 else if (flag ==1){ //mesage from bluetooth received but with incorrect passoword
  Serial.write("3"); //send wrong password message to Android
  flag=0;
 }
}

