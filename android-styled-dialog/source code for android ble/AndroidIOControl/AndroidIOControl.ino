#include "cJSON.h"
#include "communication.h"

void setup()
{
	Serial.begin(115200);          //  setup serial
	pinMode(13, OUTPUT);
}

void loop()
{
	unsigned int datLen;
	const char *data = getData(&datLen);

	if(data != NULL){
		cJSON *root = cJSON_Parse(data);
		int type = cJSON_GetObjectItem(root, "T")->valueint;
		int value = cJSON_GetObjectItem(root, "V")->valueint;
		switch(type){
			case BUZZER:{
				}break;
			case RELAY:{
				}break;
			case RGBLED:{
				int red = cJSON_GetObjectItem(root, "R")->valueint;
				int green = cJSON_GetObjectItem(root, "G")->valueint;
				int blue = cJSON_GetObjectItem(root, "B")->valueint;
			}break;
			case TEMPERATURE:{
				float temperature = 10.1;
				sendTemperature(temperature);
			}break;
			case HUMIDITY:{
				float humidity = 40.0;
				sendHumidity(humidity);
			}break;
			case DIGITAL:
			case ANALOG:{
				int pin = cJSON_GetObjectItem(root, "P")->valueint;
				int mode = cJSON_GetObjectItem(root, "M")->valueint;
				if(mode == WRITE){
					pinMode(pin, OUTPUT);
					if(type == DIGITAL){
						digitalWrite(pin, value?HIGH:LOW);
					}else{
						analogWrite(pin, value);
					}
				}else{
					pinMode(pin, INPUT);
					int value;
					if(type == DIGITAL){
						value = digitalRead(pin);
					}else{
						value = analogRead(pin);
					}
					char jsonBuf[40];
					sprintf(jsonBuf, "{\"T\":%d, \"V\":%d, \"P\":%d}", type, value, pin);
					sendData(jsonBuf);
				}
			}break;
			default:break;
		}
		cJSON_Delete(root);
	}	
}

