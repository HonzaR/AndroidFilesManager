#include "Communication.h"
#include "cJSON.h"
#include <stdio.h>

static char recvBytes[100];
static unsigned int recvBytesLength = 0;
static char jsonBuf[100];

void sendData(const char *pData){
	int i = 0;
	while(pData[i] != '\0'){
		Serial.write(pData[i++]);
	}
}


void sendTemperature(float t)
{
	sprintf(jsonBuf, "{\"T\":%d, \"V\":%d}", TEMPERATURE, (int)(t*100));
	sendData(jsonBuf);
}

void sendHumidity(float h)
{
	sprintf(jsonBuf, "{\"T\":%d, \"V\":%d}", TEMPERATURE, (int)(h*100));
	sendData(jsonBuf);
}

const char *getData(unsigned int *pLen)
{
	unsigned int bytes;
	static unsigned char haveStartChar = 0;
	static unsigned int startCharIndex = 0;
	
	if((bytes = Serial.available()) > 0){
		unsigned int currentLength = recvBytesLength;
		recvBytesLength += bytes;
		for(int i=currentLength; i<recvBytesLength; i++){
			recvBytes[i] = Serial.read();
			if(recvBytes[i] == '{' && haveStartChar == 0){
				haveStartChar = 1;
				startCharIndex = i;
			}
			if(recvBytes[i] == '}'){
				recvBytes[i+1] = '\0';
				recvBytesLength = 0;
				haveStartChar = 0;
				*pLen = recvBytesLength-startCharIndex;
				unsigned int temp = startCharIndex;
				startCharIndex = 0;
				return recvBytes+temp;
			}
		}
 	}
	*pLen = 0;
	return NULL;
}



