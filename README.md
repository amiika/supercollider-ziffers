# supercollider-ziffers
Numbered Notation for SuperCollider (Prototype)

# Install

```
Quarks.install("https://github.com/amiika/supercollider-ziffers.git");
```

# Play

```
 Ziffers.play("q .0 .0 | q 0 e 1 q. 2 | 2 e 1 q 2 e 3| h. 4| e 7 7 7 4 4 4 2 2 2 0 0 0 | q 4 e 3 q 2 e 1 | h. 0");
 
 Ziffers.loop("q .0 .0 | q 0 e 1 q. 2 | 2 e 1 q 2 e 3| h. 4| e 7 7 7 4 4 4 2 2 2 0 0 0 | q 4 e 3 q 2 e 1 | h. 0");
 
 // Create array: [[1,2,[3,1,2,3],[1,0.5,0.125]]
 a = Ziffers.parse("h 1 q 2 e 3123");
```

# Modify

```
(
SynthDef(\horror, { |out=0, freq=440, amp=0.5,force=1, gate=1,pos=0.07,c1=0.25,c3=31,pan=0|
    var vib = Gendy1.kr(10,2,9,4,0.1, 4,mul:0.110,add:2);
    var son = DWGBowed.ar(freq*vib, amp,force, gate,pos,0.1,c1,c3);
    son = DWGSoundBoard.ar(son);
    son = BPF.ar(son,490,1)+son;
    son = LPF.ar(son,9000);
    Out.ar(out, Pan2.ar(son * 0.1, pan));
}).add;


Ziffers("e 5 3 q 135 e 6 4 q 357",\foo,inf);

Pbindef(\foo,
	\instrument, \horror
).play;
)
```

# ToDo:

... a lot. Basics work but its just a prototype for now.

