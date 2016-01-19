#!/usr/bin/env python3

import subprocess
import unittest
import socket
import time
import random
import sys
import re
import pprint

TCP_APPLICATION_PORT = 7778
TCP_ANDROID_PORT = 7777
TCP_IP = '127.0.0.1'

COMMAND_TOGGLE_FILTER = 1
COMMAND_RESET_MATRIX = 2

class Server():
  process = None
  isStarted = False

  def startServer(self):
    print("TEST:\t Starting Qriro-server")
    self.process = subprocess.Popen(["../build/Qriro-server", "TCP", str(TCP_ANDROID_PORT), str(TCP_APPLICATION_PORT)])
    time.sleep(2)
    self.isStarted = True

  def stopServer(self):
    print("TEST:\t Killing Qriro-server")
    self.process.kill()
    time.sleep(2)
    self.isStarted = False


class TestQriroServerMethods(unittest.TestCase):
  # Android TCP socket
  AndroidSocket = None

  # Application TCP socket
  ApplicationSocket = None

  def setUp(self):
    # Connect Qriro-server to both a simulated Android device and a
    # simulated application with TCP sockets
    self.AndroidSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    self.AndroidSocket.connect((TCP_IP, TCP_ANDROID_PORT))

    self.ApplicationSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    self.ApplicationSocket.connect((TCP_IP, TCP_APPLICATION_PORT))

  def tearDown(self):
    # disconnect both sockets
    self.AndroidSocket.close()
    self.ApplicationSocket.close()
    time.sleep(0.5)

  def sendCommandToServer(self, commandId):
    """Send a command to Qriro.server via Android socket"""
    message = "C:0:0:" + str(commandId) + ":0:0;\n"
    self.AndroidSocket.send(str.encode(message))
    time.sleep(10./1000.)

  def sendFileToServer(self, filename, nbSamples):
    """send filename content to Qriro-server via Android socket"""
    sample = open(filename,'r')
    for i,line in enumerate(sample):
        self.AndroidSocket.send(str.encode(line))
        if i > nbSamples:
            break
        time.sleep(10./1000.) # Sleep 20 ms between samples
    sample.close()

  def sendMeasureToServer(self, Sens, ID, val):
    """ Sends a (fake) measurement from sensor Sens, with id ID, and
    three values val"""
    sampleTime = 10./1000.
    measurements = Sens + ':' + str(ID) + ':' + str(ID*sampleTime) + ':'
    measurements += str(val[0]) + ':' + str(val[1]) + ':' + str(val[2]) + ";\n"
    self.AndroidSocket.send(str.encode(measurements))
    time.sleep(sampleTime) # wait sampletime between samples

  def getMatrixFromServer(self):
    """ Asks a resulting transformation matrix to Qriro-server, emulating
    an application. Convert the answer into a float list."""
    self.ApplicationSocket.send(str.encode("GETMAT"))
    data = self.ApplicationSocket.recv(200)
    data = data.decode()
    matrixWithHeader = re.split(':', data)
    if matrixWithHeader[0] != "MAT":
        raise Exception('Error in getMatrixFromServer')
    matrixWithoutHeader = matrixWithHeader[1:]
    matrixWithoutHeader[-1] = matrixWithoutHeader[-1].replace(";\n",'')
    return [float(i) for i in matrixWithoutHeader]

  def getCommandFromServer(self):
    """ Ask if a command is available in Qriro-server"""
    self.ApplicationSocket.send(str.encode("GETCOM"))
    data = self.ApplicationSocket.recv(200)
    data = data.decode()
    commandWithHeader = re.split(':', data)
    if commandWithHeader[0] != "COM":
        print(data)
        raise Exception('Error in getCommandFromServer')
    commandWithoutHeader = commandWithHeader[1].replace(";\n",'')
    return commandWithoutHeader


  #----------
  # TEST CASES
  def test_simpleRotation_1(self):
    """Test if rotations in the angle alpha are only traduced to
    specific elements in MAT"""
    for i in range(20):
      randomNumb = random.gauss(0,1)
      val = [randomNumb, 0, 0]
      self.sendMeasureToServer("G", i, val)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.assertEqual(answer[0], 1.0)
    self.assertEqual(answer[1], 0)
    self.assertEqual(answer[2], 0)
    self.assertEqual(answer[3], 0)
    self.assertEqual(answer[4], 0)
    self.assertTrue(-1<=answer[5]<=1)
    self.assertTrue(-1<=answer[6]<=1)
    self.assertEqual(answer[7], 0)
    self.assertEqual(answer[8], 0)
    self.assertTrue(-1<=answer[9]<=1)
    self.assertTrue(-1<=answer[10]<=1)
    self.assertEqual(answer[11], 0)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_simpleRotation_2(self):
    """Test if rotations in the angle beta are only traduced to
    specific elements in MAT"""
    for i in range(20):
      randomNumb = random.gauss(0,1)
      val = [0, randomNumb, 0]
      self.sendMeasureToServer("G", i, val)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.assertTrue(-1<=answer[0]<=1)
    self.assertTrue(-1<=answer[1]<=1)
    self.assertEqual(answer[2], 0)
    self.assertEqual(answer[3], 0)
    self.assertTrue(-1<=answer[4]<=1)
    self.assertTrue(-1<=answer[5]<=1)
    self.assertEqual(answer[6], 0)
    self.assertEqual(answer[7], 0)
    self.assertEqual(answer[8], 0)
    self.assertEqual(answer[9], 0)
    self.assertEqual(answer[10], 1.0)
    self.assertEqual(answer[11], 0)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_simpleRotation_3(self):
    """Test if rotations in the angle gamma are only traduced to
    specific elements in MAT"""
    for i in range(20):
      randomNumb = random.gauss(0,1)
      val = [0, 0, randomNumb]
      self.sendMeasureToServer("G", i, val)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.assertTrue(-1<=answer[0]<=1)
    self.assertEqual(answer[1], 0)
    self.assertTrue(-1<=answer[2]<=1)
    self.assertEqual(answer[3], 0)
    self.assertEqual(answer[4], 0)
    self.assertEqual(answer[5], 1.0)
    self.assertEqual(answer[6], 0)
    self.assertEqual(answer[7], 0)
    self.assertTrue(-1<=answer[8]<=1)
    self.assertEqual(answer[9], 0)
    self.assertTrue(-1<=answer[10]<=1)
    self.assertEqual(answer[11], 0)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_complexRotation_1(self):
    """Test file Rotation_test1.dat with 32 samples"""
    self.sendFileToServer("dataSamples/Rotation_test1.dat", 32)
    answer = self.getMatrixFromServer()
    print("TEST: \t" + str(answer))
    self.assertAlmostEqual(answer[0], 0.999995)
    self.assertAlmostEqual(answer[1], -0.00275125)
    self.assertAlmostEqual(answer[2], 0.00130204)
    self.assertEqual(answer[3], 0.0)
    self.assertAlmostEqual(answer[4], 0.00275715)
    self.assertAlmostEqual(answer[5], 0.999986)
    self.assertAlmostEqual(answer[6], -0.00455659)
    self.assertEqual(answer[7], 0.0)
    self.assertAlmostEqual(answer[8], -0.00128948)
    self.assertAlmostEqual(answer[9], 0.00456015)
    self.assertAlmostEqual(answer[10], 0.999989)
    self.assertEqual(answer[11], 0)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_complexRotation_2(self):
    """Test file Rotation_test3.dat with 32 samples"""
    self.sendFileToServer("dataSamples/Rotation_test3.dat", 32)
    answer = self.getMatrixFromServer()
    print("TEST: \t" + str(answer))
    self.assertAlmostEqual(answer[0], 0.999992)
    self.assertAlmostEqual(answer[1], -0.00382816)
    self.assertAlmostEqual(answer[2], 0.000760247)
    self.assertEqual(answer[3], 0.0)
    self.assertAlmostEqual(answer[4], 0.00383191)
    self.assertAlmostEqual(answer[5], 0.99998)
    self.assertAlmostEqual(answer[6], -0.00499239)
    self.assertEqual(answer[7], 0.0)
    self.assertAlmostEqual(answer[8], -0.000741121)
    self.assertAlmostEqual(answer[9], 0.00499527)
    self.assertAlmostEqual(answer[10], 0.999987)
    self.assertEqual(answer[11], 0.0)
    self.assertEqual(answer[12], 0.0)
    self.assertEqual(answer[13], 0.0)
    self.assertEqual(answer[14], 0.0)
    self.assertEqual(answer[15], 1.0)

  def test_complexRotation_3(self):
    """Test file Rotation_test5.dat with 256 samples"""
    self.sendFileToServer("dataSamples/Rotation_test5.dat", 256)
    answer = self.getMatrixFromServer()
    print("TEST: \t" + str(answer))
    self.assertAlmostEqual(answer[0], 0.999708)
    self.assertAlmostEqual(answer[1], 0.0156394)
    self.assertAlmostEqual(answer[2], 0.0184051)
    self.assertEqual(answer[3], 0.0)
    self.assertAlmostEqual(answer[4], -0.0161389)
    self.assertAlmostEqual(answer[5], 0.999497)
    self.assertAlmostEqual(answer[6], 0.0273095)
    self.assertEqual(answer[7], 0.0)
    self.assertAlmostEqual(answer[8], -0.0179687)
    self.assertAlmostEqual(answer[9], -0.0275986)
    self.assertAlmostEqual(answer[10], 0.999458)
    self.assertEqual(answer[11], 0.0)
    self.assertEqual(answer[12], 0.0)
    self.assertEqual(answer[13], 0.0)
    self.assertEqual(answer[14], 0.0)
    self.assertEqual(answer[15], 1.0)

  def test_complexRotationFilter_1(self):
    """Test file Rotation_test5.dat with 256 samples and FIR filter
    active"""
    self.sendCommandToServer(COMMAND_TOGGLE_FILTER)
    self.sendFileToServer("dataSamples/Rotation_test5.dat", 256)
    self.sendCommandToServer(COMMAND_TOGGLE_FILTER)
    answer = self.getMatrixFromServer()
    print("TEST: \t" + str(answer))
    self.assertAlmostEqual(answer[0], 0.999976)
    self.assertAlmostEqual(answer[1], -0.00592333)
    self.assertAlmostEqual(answer[2], 0.00367834)
    self.assertEqual(answer[3], 0.0)
    self.assertAlmostEqual(answer[4], 0.00594656)
    self.assertAlmostEqual(answer[5], 0.999962)
    self.assertAlmostEqual(answer[6], -0.00633545)
    self.assertEqual(answer[7], 0.0)
    self.assertAlmostEqual(answer[8], -0.00364067)
    self.assertAlmostEqual(answer[9], 0.00635717)
    self.assertAlmostEqual(answer[10], 0.999973)
    self.assertEqual(answer[11], 0.0)
    self.assertEqual(answer[12], 0.0)
    self.assertEqual(answer[13], 0.0)
    self.assertEqual(answer[14], 0.0)
    self.assertEqual(answer[15], 1.0)

  def test_simpleTranslation_1(self):
    """Test if translations in x are only traduced to specific elements
    in MAT"""
    for i in range(20):
      randomNumb = random.gauss(0,1)
      val = [randomNumb, 0, 0]
      self.sendMeasureToServer("S", i, val)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.assertEqual(answer[0], 1.0)
    self.assertEqual(answer[1], 0)
    self.assertEqual(answer[2], 0)
    self.assertTrue(-20<=answer[3]<=20)
    self.assertEqual(answer[4], 0)
    self.assertEqual(answer[5], 1.0)
    self.assertEqual(answer[6], 0)
    self.assertEqual(answer[7], 0)
    self.assertEqual(answer[8], 0)
    self.assertEqual(answer[9], 0)
    self.assertEqual(answer[10], 1.0)
    self.assertEqual(answer[11], 0)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_simpleTranslation_2(self):
    """Test if translations in y are only traduced to specific elements
    in MAT"""
    for i in range(20):
      randomNumb = random.gauss(0,1)
      val = [0, randomNumb,  0]
      self.sendMeasureToServer("S", i, val)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.assertEqual(answer[0], 1.0)
    self.assertEqual(answer[1], 0)
    self.assertEqual(answer[2], 0)
    self.assertEqual(answer[3], 0)
    self.assertEqual(answer[4], 0)
    self.assertEqual(answer[5], 1.0)
    self.assertEqual(answer[6], 0)
    self.assertTrue(-20<=answer[7]<=20)
    self.assertEqual(answer[8], 0)
    self.assertEqual(answer[9], 0)
    self.assertEqual(answer[10], 1.0)
    self.assertEqual(answer[11], 0)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_simpleTranslation_3(self):
    """Test if translations in z are only traduced to specific elements
    in MAT"""
    for i in range(20):
      randomNumb = random.gauss(0,1)
      val = [0, 0, randomNumb]
      self.sendMeasureToServer("S", i, val)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.assertEqual(answer[0], 1.0)
    self.assertEqual(answer[1], 0)
    self.assertEqual(answer[2], 0)
    self.assertEqual(answer[3], 0)
    self.assertEqual(answer[4], 0)
    self.assertEqual(answer[5], 1.0)
    self.assertEqual(answer[6], 0)
    self.assertEqual(answer[7], 0)
    self.assertEqual(answer[8], 0)
    self.assertEqual(answer[9], 0)
    self.assertEqual(answer[10], 1.0)
    self.assertTrue(-600<=answer[11]<=600)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_resetMatrix(self):
    """Test if COMMAND_RESET_MATRIX works correctly"""
    for i in range(20):
      randomNumb = random.gauss(0,1)
      val = [randomNumb, randomNumb, randomNumb]
      self.sendMeasureToServer("S", i, val)
      self.sendMeasureToServer("G", i, val)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.sendCommandToServer(COMMAND_RESET_MATRIX)
    answer = self.getMatrixFromServer()
    print("TEST:\t" +  str(answer))
    self.assertEqual(answer[0], 1.0)
    self.assertEqual(answer[1], 0)
    self.assertEqual(answer[2], 0)
    self.assertEqual(answer[3], 0)
    self.assertEqual(answer[4], 0)
    self.assertEqual(answer[5], 1.0)
    self.assertEqual(answer[6], 0)
    self.assertEqual(answer[7], 0)
    self.assertEqual(answer[8], 0)
    self.assertEqual(answer[9], 0)
    self.assertEqual(answer[10], 1.0)
    self.assertEqual(answer[11], 0)
    self.assertEqual(answer[12], 0)
    self.assertEqual(answer[13], 0)
    self.assertEqual(answer[14], 0)
    self.assertEqual(answer[15], 1.0)

  def test_broadcastMessage(self):
    """ Test if COMMANDS with 1024 <= Id < 2048 are broadcasted from Android to
    the application"""
    self.sendCommandToServer(1024)
    answer = self.getCommandFromServer()
    print("TEST:\t " + answer)
    self.assertEqual(answer, str(1024))


if __name__ == '__main__':
    server = Server()
    server.startServer()
    suite = unittest.TestLoader().loadTestsFromTestCase(TestQriroServerMethods)
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    server.stopServer()

    if result.wasSuccessful() == True:
      print("TEST:\t Unit testing was SUCCESSFUL")
      exit(0)
    else:
      print("TEST:\t Unit testing FAILED")
      print
      exit(1)
