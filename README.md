Qriro-RV-Sample
============

Qriro is an application that can be used to translate the motion sensor information from an android device into a rotation/translation matrix (transformation matrix).
This transformation matrix can be used in other applications, such as virtual reality and games.

In this program, the transformation matrix is used in two virtual reality examples.

## Requirements
- Processing (tested with 2.2.1)
- JDK (tested with Openjdk 7)
- Qriro Server and Android working.

## What does this demo do?
First, a socket is established between the C server and this program (see CommunicationThread.java). Each time the screen is drawn, a new translation matrix is requested to the C server using `commThread.askNewRotationMatrix()`. This matrix can be used with `commThread.getRotationTranslationMatrix()`.

As specified, two examples are given:
- A Cube. whose position is modified using the transformation matrix (see Cube.java)
- A Game, where diamonds have to be caught using a ring controlled by the phone. This game is inspired in the [processing tutorial](https://github.com/AmnonOwed/P5_CanTut_GeometryTexturesShaders/tree/master/Custom3DGeometry) by [Amnon Owed](https://github.com/AmnonOwed)

If you need help or want to contribute, don't hesitate fill a bug report. 
You can also write to fclad at mecatronicauncu.org
