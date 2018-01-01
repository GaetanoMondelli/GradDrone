# GradDrone

GradDrone (Graduation Cap Drone) is a project to fly a Graduation wreath on your head. 

>In Italy, the term laureato is used in to refer to any student who has graduated. Right after the graduation ceremony, or laurea in >Italian, the student receives a laurel wreath to wear for the rest of the day. This tradition originated at the University of Padua and >has spread in the last two centuries to all Italian universities. (en.wikipedia.org/wiki/Laurel_wreath)

This project uses a Parrot Bebop Drone and extends the API example (https://github.com/Parrot-Developers/Samples). You need to place a QRCODE on a graduation cap and run the Android application. The Bebop will point its camera downward and will send the video stream to the smartphone. The android application will search for the QR-CODE and will use its coordinates and its area to send commands to the drone, adjusting its position until it will be centred upon the cap. 
The x,y coordinates are used to drift the drone left, right.forward and backwards. The area is used to decide the altitude of the drone.  

If the videostream is fast enough the drone can follow you. 

ALERT: It is not SAFE to fly a drone over your head, use caution and I'm not assuming any responsibility for its use.

