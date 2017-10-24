# Acoustic Communication
In this project we want to design a communication system which can transmit data over acoustic waves. Here, our target is smart phone which runs Android operating system and that’s because android gives us a wide range of access to hardware including determining sampling rate, different mode of playing sound on speaker. These access also applied for microphone hardware which we need on receiver side.
First we will discuss about designing transmitter and choosing a modulation scheme. Then based on that, receiver will be designed and also result of different experiments with this communication system will be reported.
# Transmitter
I decided to use On-Off Keying (OOK) as my modulation scheme which sends one bit per each sample. The reason is that at the beginning of this project, creating a sine wave on android seems to be difficult and as the modulation scheme gets more complicated, receiver will be also more complicated to design. Hence, I decided to use simplest modulation scheme and after implementing that, we can improve transmitter and use other schemes.
As figure1 shows, OOK modulation is using two different symbols to sends one bit per symbol. This modulation uses a sine wave to send a bit which is correspond to 1 and sends a signal with zero amplitude that correspond to 0. Duration of each symbol is called time interval (T) which shows length of modulated signal for each bit. Time interval is dependent to noise floor of the environment and by increasing this number, we can achieve higher SNR. Also, there is a trade-off between time interval and bitrate which should be determined based on the characteristics of communication channel.

![Alt text](/figs/modulations.jpg?raw=true "Figure 1- Different Modulation Scheme in Time Domain")


As block diagram in figure 2 shows, transmitter is structured by different submodules and each of these modules take a responsibility for transmitting a signal.

![Alt text](/figs/block.jpg?raw=true "Figure 2- Transmitter Block Diagram")


## Message Reader
This submodule reads message from file or input text field on the application.

## Header Generator
As input, this module receives message from message reader and generates proper header related to message. This header can contain several information such as transmitter address, destination address, number of bits etc.

## String to Binary
This submodule converts string message to binary and prepare information in such a way that would be readable by encoder.

## Encoder
After adding header to the message, encoder received this information in binary format and does coding process. One of the benefits of dividing transmitter to different submodules is that we can implement different coding methods based on the information that input message is carrying on. For example, if input message only contains of English alphabets and it does not include any kind of special characters, encoder can encode each character to seven bits. Because based on ASCII definition of each character, all alphabets and lots of usual characters can be shown in seven bits. In my design, because I wasn’t sure about the input message format, I decided to use eight bits coding and support all ASCII characters and English alphabets.

## Modulator
This submodule, uses input bit stream and modulate them to continues sine wave. Because of using a hierarchy system in communication, it is easy to change this submodule and use any kind of modulation scheme such as On-Off keying, ASK, PSK etc. Wave generator produces a sine wave based on input bit stream and by adding sync pattern at the beginning of the wave, information will be transmitted by acoustic wave.

## Sync Pattern
Synchronization pattern contains two parts. The beginning of this pattern is a 13-Barker code preamble and second part is thirteen symbols which all of them correspond to one. The reason that I choose barker code as preamble is that by doing correlation between received signal and a match filter, related to 13-Barker code pattern, one high pick signal will be generated which can be used to detect the beginning of the message on received signal. 13-Barker code pattern shown in figure 3.
Table 1- Known Barker Codes

| Length | Codes | Sidelobe level ratio |
| -------------  | ------------- | ------------- |
| 2  | +1 −1 or +1 +1 				| −6 dB		|
| 3  | +1 +1 −1 				| −9.5 dB	|
| 4  | +1 +1 −1 +1	or +1 +1 +1 −1 		| −12 dB	|
| 5  | +1 +1 +1 −1 +1				| −14 dB	|
| 7  | +1 +1 +1 −1 −1 +1 −1 			| −16.9 dB	|
| 11 | +1 +1 +1 −1 −1 −1 +1 −1 −1 +1 −1 	| −20.8 dB	|
| 13 | +1 +1 +1 +1 +1 −1 −1 +1 +1 −1 +1 −1 +1 	| −22.3 dB	|


At best case, barker code can provide 22.3 dB side lobe level ratio, according to table 1, which is a very promising value as a preamble. Although, length of this preamble is long compare to other preambles and that adds up to the overhead of each message, but there is a trade-off between length of preamble and bit error rate(BER). By decreasing the size of the preamble, probability of missing the beginning of each message would increase and that cause more BER. Exploring characteristics of the channel which this communication system would be used in, can give us more hints about this trade-off. Unfortunately, I did not have enough time to try different preamble patterns and do experiments on them, but as table 1 shows, shorter preamble also can be used. In that case, bit rate can be improved by decreasing the length of preamble or using other preamble patterns.

![Alt text](/figs/barker.jpg?raw=true "Figure 3- 13-Barker Code Pattern")


After sending barker code, a pattern of 13 symbols all equals to one is sending which is used for energy detection and channel estimation. The number of thirteen is chosen completely random and could be changed depend on the channel characteristic and average distance between transmitter and receiver. Receiving this pattern on receiver would be useful to update threshold value and based on that demodulated bits from received signal.
 
# Receiver
Receiver side of a communication system is completely dependent to the transmitter side. Receiver should be able to receive signal with an ADC and demodulate it based on a protocol that was already defined between transmitter and receiver. Figure 3 shows a block diagram of our receiver.

![Alt text](/figs/receiver.jpg?raw=true "Figure 4- Receiver Block Diagram")

## ADC
ADC submodule receives signal in analog domain and convert it to digital values.

## Bandpass Filter
Bandpass filter extracts signal on carrier frequency with a limited bandwidth. In this project I set carrier frequency on 10KHz. Majority of sounds that we observe in environment are less than 4KHz. Also, speaker and microphone on android phones does not seem to have a good response above 12 KHz. Therefore, I choose 10 KHz as carrier frequency and also bandpass filter is designed in such a way that could pass frequencies around 10 KHz with a specified bandwidth.

## Low pass Filter
Low pass filter eliminates frequencies that are greater than the bandwidth of our modulated message. Suppose that each symbol is modulated in 100 samples. Considering sampling rate which is 44100 Hz, that means each symbol duration is about 2.2ms. Therefore, the frequency of received signal is about 441 Hz. Considering that modulated signal is a square wave, we should design a low pass filter that pass at least 4 times of the signal frequency, to receive major harmonics.

## Sync Detector
This submodule uses sync pattern to identify the beginning of message and that’s called synchronization. By doing that, starting point of message is send to demodulator to demodulate message bits.

## Demodulator
This submodule, receives filtered wave and starting point of message, then it demodulates signal based on the modulation scheme that has been already defined between transmitter and receiver.

## Decoder
After demodulation, a bit stream will be send to the decoder to decode bits. This submodule is also apply decoding based on the encoding process that has been done in encoder on receiver side.

## Binary to String
At the end of the receiver system, this binary to string submodule receives decoded bit stream of message and translate that to string. Message will be shown as output of this submodule.
 
# Experiments
Transmitter part is this communication system is completely designed in java programming. From reading message from textbox to modulating message to wave and transmitting on speaker.
Receiver side first was designed in Matlab. Signal was recorded with receiver application on another android phone and stored in a file. At the beginning, I explored the spectrum of received signal in Matlab to figure out what kind of filtering process should be done. After designing filters, I wrote a complete program for receiver side on Matlab which can read signal from file and decode message. Having that, I was sure that my system works fine and then I tried to optimize it by decreasing length of each symbol.
After writing receiver side in Matlab, I started to transfer demodulating process to Java programming on android. Considering that doing digital signal processing in Java programming is not efficient, the challenge was designing better filters that can reduce the cost of filter in terms of time. By comparing different kinds of filters, using FIR filter seems to be more efficient. I choose Kaiser window FIR filter which provides filter coefficient in numerator and denominator is always equals to one. Therefore, complexity of the filter would be Filter length*Number of samples and because length of filter is limited to less than hundred here, complexity would be Ο(n). Although, this complexity is correct when filter’s length is much less than the number of samples!
My receiver uses three different filters which are bandpass filter, lowpass filter and match filter. In designing filter, there are some options that are adjustable and really affects filter length. Bandwidth of filter, margin between stop frequencies and start frequencies and also attenuation value for stop band frequencies can change filter’s length. In this case, I tried couple of different configuration to minimize filter’s length.
Bigger challenge in designing filters was matched filter for detecting sync pattern. Generally, matched filter for detecting any pattern is a conjugated time-reversed version of the signal pattern. Which means match filter has same length that sync pattern has. In case of using symbol length of 80 and 100 and using 13-Barker code, matched filter length would be 1040 and 1300 samples. Doing convolution between this large filter and signal is so time consuming which makes my receiver so slow.
As I mentioned, receiver side is working so slow and I spend some time and improved it, but still needs to find more efficient convolution algorithm for Java programming. For my setup, it took about 5 to 10 minutes to decode a 1Kb message which is pretty slow for doing different experiments. Therefore, I tested receiver application with different messages and works fine, but for doing different experiments, I generated signal with my transmitter application on one phone, record signal on another phone with my receiver application and then I used Matlab receiver code to decode message! Receiver side on Matlab and Java programming are completely same and only reason that I used Matlab was because of my limited time in doing these experiments. My set up for this communication system is explained here.

### Modulation scheme
Modulation scheme is On-Off Keying(OOK).
### Number of bits per symbol
OOK can have two different patterns in each symbol which means it can carry one bit per symbol.
### Bandpass Filter
I designed a filter that passes frequencies between 8.5KHz to 11.5KHz which provides sufficient bandwidth for different length of symbol like 100 or 80 samples per symbol.
### Lowpass Filter
I designed two different filter based on different length of symbol. For the length equals to 100 samples/symbol, filter is passing frequencies less than 2KHz, whereas for the length of 80 samples/symbol, filter is designed to pass frequencies less than 2.5KHz! If we increase length of each symbol, we need to increase the lowpass filter bandwidth, because we are using more bandwidth in our modulated signal.
### Data rate
By using this set up, I was able to reduce each symbol length to 80 samples. Considering that sampling rate is 44100, data rate would be around 551 bit per second. We also have 26 symbols (barker code and energy detection pattern) overhead for each transmission, which reduces data rate to 537 bit per second. These numbers are valid when message length is 1Kb. By sending longer message, the effect of overhead on bit rate would be less.
### Bit error rate
As figure 5 shows, BER is increasing by having more distance between two phones.

![Alt text](/figs/result.jpg?raw=true "Figure 5- Bit Error Rate versus Distance")


### Maximum distance with BER less than 2%
Based on figure 5, maximum distance that this communication system can transfer data with BER less than 2% is about 20cm. 












