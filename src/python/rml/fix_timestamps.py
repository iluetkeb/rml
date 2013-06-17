import subprocess
import sys
from math import sqrt
import time
import wave
import operator
from datetime import datetime
import numpy
from yaafelib import FeaturePlan, Engine
import matplotlib.pyplot as plot
import scipy.io.wavfile as wavfile

converter = {'Encoded date' : lambda(x) : time.mktime(datetime.strptime(x, "%Z %Y-%m-%d %H:%M:%S").timetuple())}

fpMFCC = FeaturePlan(sample_rate=16000, normalize=True)
fpMFCC.addFeature('mfcc: MFCC blockSize=512 stepSize=256')
engine = Engine()
engine.load(fpMFCC.getDataFlow())

def getTextUnderNode(node):
    rc = []
    
    for child in node.childNodes:
        if child.nodeType == node.TEXT_NODE:
            rc.append(child.data.replace('\n', '').strip())
        else:
            rc.append(getTextUnderNode(child).strip())
    
    return ' '.join(rc)

def visualizeResult(stream1, stream2, rate, bestPosition, distances, filename):
    plot.subplot(3, 1, 1)
    plot.plot(map(lambda(x): float(x) / rate, range(len(stream1))), stream1)
    plot.subplot(3, 1, 2)
    bestPosition = float(bestPosition) / rate
    plot.plot(map(lambda(x): float(x) / rate, range(len(stream2))), stream2)
    plot.plot([bestPosition, bestPosition], [min(stream2), max(stream2)])
    plot.subplot(3, 1, 3)
    plot.bar(map(lambda(x) : x['position'], distances), map(lambda(x) : x['value'], distances))
    plot.savefig(filename)
    plot.close()

def writeWave(outFileName, frames, rate):
    wavfile.write(outFileName, rate, frames)

def getMetaData(mediafile):
    proc = subprocess.Popen(['/vol/ai/ffmpeg/bin/mediainfo',mediafile],stdout=subprocess.PIPE)
    
    metadata = {}
    key = ""
    
    for line in iter(proc.stdout.readline,''):
        line = line.strip()
        delimPos =line.find(':')
        if delimPos == -1 and line != '':
            key = line
            metadata[key] = {}
        if key != ""  and delimPos != -1:
            subkey = line[:delimPos].strip()
            value = line[delimPos + 1:].strip()
            if subkey in converter:
                value = converter[subkey](value)
            metadata[key][subkey] = value

    return metadata

def saveZip(a, b):
    if len(a) != len(b):
        print "Error: saveZip(): length unequal!"
    else:
        return zip(a, b)

def findBestMFCCPosition(vectorToSearch, vectorToSearchIn, stepWidth):
    arr = numpy.array(vectorToSearch, float)
    arr = arr.reshape((1, len(vectorToSearch)))
    features = engine.processAudio(arr)
    featsToSearch = features["mfcc"] 
    engine.reset()
    
    arr = numpy.array(vectorToSearchIn, float)
    arr = arr.reshape((1, len(vectorToSearchIn)))
    features = engine.processAudio(arr)
    featsToSearchIn = features["mfcc"]
    engine.reset()
    
    startPosition = 0
    endPosition = len(featsToSearchIn) - len(featsToSearch) + 1
    positions = range(startPosition, endPosition)
    
    distances = []
    
    sliceLength = float(len(featsToSearch))

    for position in positions:
        featureSliceToCompare = featsToSearchIn[position:position + sliceLength]
        
        dist = sum(map(lambda(a, b) : numpy.linalg.norm(a - b), zip(featsToSearch, featureSliceToCompare)))
        
        distances.append({'position' : position * 256, 'value' : dist / sliceLength})
    
#    relDist = []
#    neighbourhood = 15
#    for i in range(neighbourhood,len(distances) - neighbourhood):
#        dist = distances[i]
#        
#        # left side distances
#        rd = 0.0
#        for d in distances[i-neighbourhood:i+neighbourhood]:
#            rd = rd + (dist['value'] - d['value'])

#        relDist.append({'position' : dist['position'], 'value' : rd})
#    
#    distances = relDist

    distances.sort(key=lambda(x) : x["value"], reverse = False)
    
    return min(distances, key=lambda(a) : a['value']), distances

def findBestCorrelatePosition(vectorToSearch, vectorToSearchIn, stepWidth):
    startPosition = 0
    endPosition = len(vectorToSearchIn) - len(vectorToSearch) + 1

    distances = []
    positions = range(startPosition, endPosition, stepWidth)

    fft1 = numpy.angle(numpy.fft.fft(vectorToSearch))

    sliceLength = len(vectorToSearch)
    normalize = numpy.correlate(fft1, fft1)

    for position in positions:
        vectorSliceToCompare = vectorToSearchIn[position:position + sliceLength]
        fft2 = numpy.angle(numpy.fft.fft(vectorSliceToCompare))
        
        dist = numpy.correlate(fft1, fft2) / normalize
        
        distances.append({'position' : position, 'value' : dist})

    distances.sort(key=lambda(x) : x["value"], reverse = True)

    return max(distances, key=lambda(a) : a['value']), distances
