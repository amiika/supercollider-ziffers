# supercollider-ziffers
Numeric uzulang for generating event patterns using the SuperColliders versatile Pbindef, Scale and Tuning implementation. [Original Ziffers](https://github.com/amiika/ziffers) was developed for Sonic Pi. This version supports both classic notation with the duration characters and new base 62 notation that unlocks the numeric notation for microtonal scales. 

# Install

```
Quarks.install("https://github.com/amiika/supercollider-ziffers.git");
```

# Basic use

Ziffers is a factory for event patterns and can be used to manipulate patterns on the fly just like with the Pbindef.

```
// Play major scale with default synth
Ziffers("0 1 2 3 4 5 6 7").play;

// Run a quantized live loop
Ziffers("1/8 0 4 3 6 1 5 6 2", \scale, 1591, \quant, 4).loop;

// Unnamed Ziffers defaults to Pbindef named z1
Ziffers("1/8 0 3 2 1/4 3 6 1/2 4 2", \scale, 1193).toPbindef;

// Get array of [[degrees], [durations], [octaves]]
Ziffers.parse("1/4 0 ^ 1 0.5 2 _ 3");

```

# Play with SynthDefs

```

// Create custom synth and pattern named foo
(
SynthDef(\square, { |out=0, freq=440, amp=0.1| 
    var env = EnvGen.kr(Env.perc, doneAction: 2);
    var sig = Pulse.ar(freq, 0.5) * amp * env;
    Out.ar(out, sig ! 2);
}).add;

Ziffers("0 1 2 3 4", \foo, \instrument, \square, \amp, 0.5, \scale, 823).play;
)

// Some horror synth
(
SynthDef(\horror, { |out=0, freq=440, amp=0.5,force=1, gate=1,pos=0.07,c1=0.25,c3=31,pan=0|
    var vib = Gendy1.kr(10,2,9,4,0.1, 4,mul:0.110,add:2);
    var son = DWGBowed.ar(freq*vib, amp,force, gate,pos,0.1,c1,c3);
    son = DWGSoundBoard.ar(son);
    son = BPF.ar(son,490,1)+son;
    son = LPF.ar(son,9000);
    Out.ar(out, Pan2.ar(son * 0.1, pan));
}).add;

Ziffers("1 2 3", \foo, \instrument, \horror).loop;
)
```

# Use with standard Pbindef

```
(
SynthDef(\simple, { |out=0, freq=440|
    var env = EnvGen.kr(Env.perc, doneAction: 2);
    Out.ar(out, SinOsc.ar(freq, 0, 0.05) * env ! 2);
}).add;

// Create mutable event pattern
Pbindef(\bar,\instrument,\simple);

// Add degrees and durations to the existing event pattern
Ziffers("0.5 0 4 2 6", \bar).play;
)
```


# Use with midi

Sending patterns with SuperCollider midi client

```
// MIDI setup
(
MIDIClient.init;
~m = MIDIOut.new(0);
~m.latency = 0.2;
CmdPeriod.add({ (0..15).do({ |ch| ~m.allNotesOff(ch) }) });
)

// Some 12EDO minor scale
Ziffers("0 4 1 024 1/8 2 4 1/4 468 3", \meh, \type, \midi, \midiout, ~m, \chan, 0, \scale, 1485, \edo, 12).loop;

// Some 14EDO scale with pitch bend tuning
Ziffers("1/8 0 4 2 4 5 2 3 9 3", \daa, \type, \midiBend, \midiout, ~m, \chan, 0, \scale, 6553, \edo, 14).loop;

// Send chords to separate channels with pitch bends
Ziffers("0 4 1 236 1/8 3 4 1/4 b 256 1/8 4 a", \huh, \multi, true, \type, \midiBend, \midiout, ~m, \chan, 0, \scale, 65535, \edo, 16).loop;
```

# Classic use

In classic Ziffers notation characters mostly represent durations and rest is *r*.

```
 Ziffers.classic("q. 0 q. 0 | q 0 e 1 q. 2 | 2 e 1 q 2 e 3| h. 4| e 7 7 7 4 4 4 2 2 2 0 0 0 | q 4 e 3 q 2 e 1 | h. 0").play;

 Ziffers.classic("q .0 .0 | q 0 e 1 q. 2 | 2 e 1 q 2 e 3| h. 4| e 7 7 7 4 4 4 2 2 2 0 0 0 | q 4 e 3 q 2 e 1 | h. 0").loop;

 // Returns Pbindef
 Ziffers.classic("q 1 3 2 4 5 4 3 2",\foo).toPbindef;

 Ziffers.classic("q 1 3 2 4 5 4 3 2").play;

 Ziffers.classic("q 1 3 2 4 5 4 3 2").loop;

 // Create array: [[1,2,[3,1,2,3],[1,0.5,0.125]]
 a = Ziffers.parseClassic("h 1 q 2 e 3123");
```

Classic with midi:
```
(
MIDIClient.init;
~m = MIDIOut.new(0);
~m.latency = 0.2;
CmdPeriod.add({ (0..15).do({ |ch| ~m.allNotesOff(ch) }) });

Ziffers.classic("e 0 1 d 2 3 q 5 3 w 2 1 3", \z1, \midiout, ~m, \chan, 0).play;
)
```

# ToDo:

... still a lot. This version was inspired by a microtonal midi piece for Virus TI2. For classic ziffers still only basic stuff like the degrees, note length characters, ties, rests and chords work.

