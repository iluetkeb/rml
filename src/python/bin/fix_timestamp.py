#!/usr/bin/python
from fixtimestamps import *
import sys
import re
import numpy
import wave
import struct
from logging import Logger
import scipy.io.wavfile as wavfile
import xml.dom.minidom as dom

ALIGNMENT_STRATEGY = "mfcc_512"

ALIGN_FUNCTIONS = { "mfcc_512"  : findBestMFCCPosition,
                    "acorr" : findBestCorrelatePosition }

if __name__ == '__main__':
    # pattern for parsing timestamp from small audio files
    timeRegex = re.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3}-([0-9]{13})")
    searchNeighbourhood = 10 # seconds
    searchNeighbourhoodStepWidth = 0.05 # seconds
    
    if len(sys.argv) < 3:
        print "Usage:", sys.argv[0], "<audioFile>", "<timestamp>", "<list of audiofiles with specific filename format>"
        exit(0)
    
    resultFile = sys.argv[3]

    # get timestamp
    largeFileTimeStamp = float(sys.argv[2])
    
    largeAudioFileName = sys.argv[1]

    # get audio file, non-streaming
#    frameRate, allLargeFrames = wavfile.read(largeAudioFileName)
    
#    numLargeAudioFrames = len(allLargeFrames)

    # get audio file, streaming
    largeAudioFile = wave.open(largeAudioFileName)
    sampwidth = largeAudioFile.getsampwidth()
    fmts = (None, "=B", "=h", None, "=l")
    fmt = fmts[sampwidth]
    dcs  = (None, 128, 0, None, 0)
    dc = dcs[sampwidth]

    frameRate = largeAudioFile.getframerate()
    numLargeAudioFrames = largeAudioFile.getnframes()

    searchNeighbourhoodFrames = int(searchNeighbourhood * frameRate)
    searchNeighbourhoodStepWidthFrames = int(searchNeighbourhoodStepWidth * frameRate)    
    
    largeAudioDuration = float(numLargeAudioFrames) / frameRate
    
    resultPath = ""
    if largeAudioFileName.rfind('/') != -1:
        resultPath = largeAudioFileName[:largeAudioFileName.rfind('/')] + "/"
    
    # go through all given audio files
    for fileName in sys.argv[2:]:
        try:
            smallFileName = fileName.split('/')[-1]
            
            r = timeRegex.search(smallFileName)
            
            # name matches pattern
            if r:
                dotPos = fileName.rfind('.')
                smallFileNameExtensionless = fileName[:dotPos]
                
                smallFileTimeStamp = int(r.groups()[0])
                
                timeOffset = float(smallFileTimeStamp) - largeFileTimeStamp * 1000
                
                if timeOffset > 0 and timeOffset / 1000 < largeAudioDuration:
                    # read frames from small file
                    
                    rate = frameRate
                    rate, smallFrames = wavfile.read(fileName)
                    if rate != frameRate:
                        print "Framerate of", smallFileName, "does not match!"
                    
                    numSmallFileFrames = len(smallFrames)
                    
                    smallAudioDuration = float(numSmallFileFrames) / frameRate
                    
                    # extract frames from large audio file
                    proposedPosition = int(timeOffset / 1000 * frameRate)
                    
                    startFrame = proposedPosition
                    startFrame = int(max(0, startFrame - searchNeighbourhoodFrames))
                    
                    endFrame = proposedPosition + len(smallFrames)
                    endFrame = int(min(numLargeAudioFrames, endFrame + searchNeighbourhoodFrames))
                    
                    # non-streaming way
    #                largeFrames = allLargeFrames[startFrame : endFrame]
                    # streaming way
                    largeAudioFile.setpos(startFrame)
                    
                    largeFrames_ = []
                    for i in range(endFrame - startFrame):
                        frame = largeAudioFile.readframes(1)
                        frame = struct.unpack(fmt, frame)[0]
                        frame -= dc
                        
                        largeFrames_.append(frame)
    
                    largeFrames = numpy.array(largeFrames_, numpy.int16)
                    
                    print "Extracted data, starting brute force search for best position"
                    
                    optimum, distances = ALIGN_FUNCTIONS[ALIGNMENT_STRATEGY](smallFrames, largeFrames, searchNeighbourhoodFrames)
    
                    print "Finished brute force search! Result:", optimum["value"]
    
                    # extract found position frames
                    bestPosition = optimum["position"]
                    #bestPosition = proposedPosition - startFrame
                    extractedFrames = largeFrames[bestPosition - frameRate:bestPosition + len(smallFrames) + frameRate]
    
                    outPath = ""
                    if fileName.rfind('/') != -1:
                        outPath = fileName[:fileName.rfind('/')] + "/"
    
                    proposedOffset = int(proposedPosition * 1000 / frameRate)
                    startOffset = int((bestPosition + startFrame) * 1000 / frameRate)
                    startTimestamp = int(largeFileTimeStamp * 1000) + startOffset
    
    #                outFileName = smallFileNameExtensionless + "_" + ALIGNMENT_STRATEGY + "_1.wav"
    #                writeWave(outFileName, extractedFrames, frameRate)
                    
    #                outFileName = smallFileNameExtensionless + "_" + ALIGNMENT_STRATEGY + "_2.wav"
    #                writeWave(outFileName, smallFrames, frameRate)
    
    #                outFileName = smallFileNameExtensionless + "_" + ALIGNMENT_STRATEGY + ".png"
    #                visualizeResult(smallFrames, largeFrames, frameRate, bestPosition, distances, outFileName)
                    
                    dotpos = smallFileName.rfind(".")
                    resultFilenameBase = smallFileName[:dotpos]

                    resultLog = open(resultPath + resultFilenameBase + "_" + ALIGNMENT_STRATEGY + "_aligned.tsv", "w")
                    unalignedLog = open(resultPath + resultFilenameBase + "_" + ALIGNMENT_STRATEGY + "_unaligned.tsv", "w")

                    maryxml = dom.parse(smallFileNameExtensionless + ".xml")
                    
                    text = getTextUnderNode(maryxml.firstChild)
                    line = unicode(startOffset) + u"\t" + unicode(startOffset + int(smallAudioDuration * 1000)) + u"\t" + u"\"" + unicode(text) + u"\"\n"
                    resultLog.write(line.encode("UTF-8"))
    
                    line = unicode(proposedOffset) + u"\t" + unicode(proposedOffset + int(smallAudioDuration * 1000)) + u"\t" + u"\"" + unicode(text) + u"\"\n"
                    unalignedLog.write(line.encode("UTF-8"))

                    resultLog.flush()
                    unalignedLog.flush()
                    resultLog.close()
                    unalignedLog.close()
    
#                    line = unicode(smallFileName + " | " + str(int(smallAudioDuration * 1000)) + " | " + str(startOffset) + " | " + str(distances[0]["value"]) + " | " + str(distances[1]["value"]) + "\n")
#                    otherLog.write(line.encode("UTF-8"))
#                    otherLog.flush()
                else:
                    print "Timestamp of", smallFileName, "not inside video data!"
        except Exception as e:
            print "Failed:", e
