#ifndef _COMMUNICAT_H_
#define _COMMUNICAT_H_

#define DIGITAL 		0
#define ANALOG  		1
#define OLED			2
#define RGBLED			3
#define JOYSTICK		4
#define ACCMPU6050		5
#define BUZZER			6
#define RELAY			7
#define TEMPERATURE		8
#define HUMIDITY		9
#define INFORMATION		10

#define WRITE			0
#define READ			1

extern void sendData(const char *pData);
extern void sendTemperature(float t);
extern void sendHumidity(float h);
extern const char *getData(unsigned int *pLen);

#endif

