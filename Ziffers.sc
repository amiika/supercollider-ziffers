Ziffers {
	var <key, <melody, <pairs, <isClassic;

	*new { |melody, key=\z1 ... pairs|
		^super.newCopyArgs(key, melody, pairs, false);
	}

	*classic { |melody, key=\z1 ... pairs|
		^super.newCopyArgs(key, melody, pairs, true);
	}

	*play { |melody, key=\z1 ... pairs|
		^this.new(melody, key, *pairs).play;
	}

	*loop { |melody, key=\z1 ... pairs|
		^this.new(melody, key, *pairs).loop;
	}

	*stop { |key|
		^Pbindef(key).stop;
	}

	play {
		^this.prAsPbindef(1).play;
	}

	loop {
		^this.prAsPbindef(inf).play;
	}

	stop {
		^Pbindef(key).stop;
	}

    toPbindef {
		^this.prAsPbindef(1);
	}

	prAsPbindef { |repeats=1|
		var dict = pairs.asEvent;
		var edo = dict[\edo] ?? { 12 };
		var scaleMask = dict[\scaleMask] ?? { 2741 };
		var rep = dict[\times] ?? { repeats };
		var parsed = if(isClassic) { Ziffers.parseClassic(melody) } { Ziffers.parse(melody) };
		var pbPairs, finalScale;

		if(dict[\scale].notNil) {
			if(dict[\scale].isInteger) {
				finalScale = Ziffers.prScl(dict[\scale], edo);
			} {
				finalScale = dict[\scale];
			};
		} {
			finalScale = Ziffers.prScl(scaleMask, edo);
		};

		pbPairs = [
			\scale, finalScale,
			\stepsPerOctave, edo,
			\degree, Pseq(parsed[0], rep),
			\dur, Pseq(parsed[1], rep)
		];

		if(parsed.size > 2) {
			pbPairs = pbPairs ++ [\octave, Pseq(parsed[2], rep)];
		};

		pairs.pairsDo { |k, v|
			if(k != \scale and: { k != \scaleMask } and: { k != \edo } and: { k != \times }) {
				pbPairs = pbPairs ++ [k, v];
			};
		};

		^Pbindef(key, *pbPairs);
	}

	*initClass {
		// Midi pitch bend tuning for microtonal tunings
		Event.addEventType(\midiBend, { |server|
			var freqs = ~freq.value.asArray;
			var chan, bRange, multi, midiNotes, outChans;

			if(freqs.size > 0 and: { ~midiout.notNil }) {
				chan = ~chan ? 0;
				bRange = ~bendRange ? 2.0;
				multi = ~multi ? false;

				if(multi) {
					midiNotes = [];
					outChans = [];

					freqs.do { |f, i|
						var midiFloat = f.cpsmidi;
						var baseNote = midiFloat.floor.asInteger;
						var frac = midiFloat - baseNote;
						var bendVal = 8192 + ((frac / bRange) * 8191);
						var currentChan = chan + i;

						~midiout.bend(currentChan, bendVal.clip(0, 16383).asInteger);

						midiNotes = midiNotes.add(baseNote);
						outChans = outChans.add(currentChan);
					};

					~midinote = midiNotes;
					~chan = outChans;
				}
				{
					var firstMidiFloat = freqs[0].cpsmidi;
					var firstBaseNote = firstMidiFloat.floor.asInteger;
					var frac = firstMidiFloat - firstBaseNote;
					var bendVal = 8192 + ((frac / bRange) * 8191);

					~midiout.bend(chan, bendVal.clip(0, 16383).asInteger);
					~midinote = freqs.collect { |f| f.cpsmidi.floor.asInteger };
				};
			};

			~type = \midi;
			Event.default.eventTypes[\midi].value(server);
		});
	}

	// Parser with Base62 degrees. Supports only fraction or decimal notation for durations.
	*parse {|melody|
		var melodyArray = melody.split($ ).reject(_.isEmpty);
		var noteLength = 0.25;
		var oct = 5;
		var degrees = [];
		var durs = [];
		var octaves = [];

		melodyArray.do({|item, i|
			case
				{ item == "|" } {
					noteLength = 0.25;
					oct = 5;
				}
				{ item.includes($/) } {
					var parts = item.split($/);
					if(parts.size == 2, {
						noteLength = parts[0].asFloat / parts[1].asFloat;
					}, {
						noteLength = item.asFloat;
					});
				}
				{ item.every(_ == $.) } {
					item.size.do {
						degrees = degrees.add(Rest(0));
						durs = durs.add(noteLength);
						octaves = octaves.add(oct);
					};
				}
				{ item.contains(".") and: { item.every({ |c| c.isDecDigit or: (c == $.) }) } } {
					noteLength = item.asFloat;
				}
				{ item.every({|c| c == $^ or: (c == $_) }) } {
					item.do {|char|
						if(char == $^, { oct = oct + 1 }, { oct = oct - 1 });
					};
				}
				{ true } {
					var parsedDegrees = [];
					var parsedOctaves = [];
					var acc = 0.0;
					var localOct = oct;

					item.do {|char|
						case
							{ char == $^ } { localOct = localOct + 1; }
							{ char == $_ } { localOct = localOct - 1; }
							{ char == $# } { acc = acc + 0.1; }
							{ char == $& } { acc = acc - 0.1; }
							{ char.isAlphaNum } {
								var val = this.prBase62(char);
								if(acc != 0.0) { val = val + acc };
								parsedDegrees = parsedDegrees.add(val);
								parsedOctaves = parsedOctaves.add(localOct);
								acc = 0.0;
								localOct = oct;
							};
					};

					if(parsedDegrees.size > 0, {
						if(parsedDegrees.size > 1, {
							degrees = degrees.add(parsedDegrees);
							octaves = octaves.add(parsedOctaves);
						}, {
							degrees = degrees.add(parsedDegrees[0]);
							octaves = octaves.add(parsedOctaves[0]);
						});

						durs = durs.add(noteLength);
					});
				};
		});

		^[degrees, durs, octaves]
	}

	// Parser with classic durations like in the original: https://github.com/amiika/ziffers/wiki/Melody#list-of-all-note-length-characters
	*parseClassic {|melody|
		var noteLengths = (
			'm': 8.0,
			'l': 4.0,
			'd': 2.0,
			'n': 8.0/3.0,
			'w': 1.0,
			'k': 4.0/3.0,
			'h': 0.5,
			'c': 2.0/3.0,
			'p': 1.0/3.0,
			'q': 0.25,
			'e': 0.125,
			'g': 1.0/6.0,
			's': 0.0625,
			'a': 1.0/12.0,
			't': 0.03125,
			'f': 1.0/24.0,
			'u': 0.015625,
			'x': 1.0/48.0,
			'o': 1.0/128.0,
			'y': 1.0/96.0,
			'j': 1.0/192.0,
			'z': 0.0
		);

		var melodyArray = melody.split($ );
		var noteLength = 1.0;
		var tieLength = 0.0;
		var degrees = [];
		var dots = 0;
		var dotMult = 1.0;
		var durs = [];

		melodyArray.do({|item, i|
			var parsedLengths = [];
			var parsedDegrees = [];

			// Calculate dotted note length
			dots = dots + item.findAll(".").size;

			if(dots>0,{dotMult = (2.0-(1.0/(2*dots)));});

			// Adds rest: Should be optimized to if then else if ... with what syntax?
			if(item=="r", {
				degrees = degrees.add(Rest(0));
				durs = durs.add(noteLength*dotMult);
				dots = 0;
				dotMult = 1.0;
			});

			// Parse lengths
			parsedLengths = item.findRegexp("[mklpdcwyhnqaefsxtguj]").collect{|n,i| n[1]};

			case
				{parsedLengths.size==1} {
					noteLength = noteLengths.at(parsedLengths[0].asSymbol);
				}
				{parsedLengths.size>1} {
					parsedLengths.do({|len,i|
						tieLength = tieLength + noteLengths.at(len.asSymbol);
					});
				};

			// Parse degrees
			parsedDegrees = item.findRegexp("#{0,}b{0,}-?[0-9]")
								 .collect{|n,i| n[1]}
								 .collect{|n,i| this.prParseClassicFlatsAndSharps(n);};

			if(parsedDegrees.size>0,{
				if(parsedDegrees.size>1,
				{degrees = degrees.add(parsedDegrees)},
				{degrees = degrees.add(parsedDegrees[0])});

				// Add duration for degree/chord
				if(tieLength>0.0,{
					durs = durs.add(tieLength*dotMult);
					tieLength = 0.0;
				},{
					durs = durs.add(noteLength*dotMult);
				});

				dots = 0;
				dotMult = 1.0;

			});

			// Continue loop
		});

		^[degrees,durs]
	}

	*prParseClassicFlatsAndSharps {|stringNote|
		var floatDegree = 0.0;
		if("#|b".matchRegexp(stringNote),{
			var list = stringNote.findRegexp("#|b|[0-9]").collect{|n| n[1]};
			list.do({|c|
				case
					{c=="b"} {floatDegree=floatDegree-0.1;}
					{c=="#"} {floatDegree=floatDegree+0.1;}
					{"[0-9]".matchRegexp(c.asString)==true} {floatDegree=floatDegree+c.asFloat;}
			});
			^floatDegree;
		},
		{
			^stringNote.asInteger
		});
	}

	// Support for binary scale numbers: https://ianring.com/musictheory/scales/ extended to any equal temperament scale
	*prScl {|k=2741, e=12|
		var arr = [];
		e.do { |i| if ((k >> i).bitAnd(1) == 1) { arr = arr.add(i); }; };
		^Scale.new(arr, e, Tuning.et(e));
	}

	// Characters interpreted as Base62 which enables writing simple melodies up to 62 EDO
	*prBase62 {|char|
		var c = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		var idx = c.indexOf(char);
		if(idx.notNil, { ^idx }, { ^0 });
	}
}