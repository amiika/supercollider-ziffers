Ziffers {
	*new {|melody,name=\z1,times=1|
		^this.prAsPbindef(name,this.parse(melody),times);
	}
	
	*play {|melody,name=\z1|
		^this.prAsPbindef(name,this.parse(melody)).play;
	}
	
	*loop {|melody,name=\z1|
		^this.prAsPbindef(name,this.parse(melody),inf).play;
	}
	
	*stop {|name|
		^Pbindef(name).stop;
	}

	*parse {|melody|
		var noteLengths = (
			'm': 8.0,
			'k': 5.333333333333333,
			'l': 4.0,
			'p': 2.666666666666667,
			'd': 2.0,
			'c': 1.333333333333333,
			'w': 1.0,
			'y': 0.6666666666666667,
			'h': 0.5,
			'n': 0.3333333333333333,
			'q': 0.25,
			'a': 0.1666666666666667,
			'e': 0.125,
			'f': 0.0833333333333333,
			's': 0.0625,
			'x': 0.0416666666666667,
			't': 0.03125,
			'g': 0.0208333333333333,
			'u': 0.015625,
			'j': 0.0104166666666667
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
								 .collect{|n,i| this.prParseFlatsAndSharps(n);};
	
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
	
	*prParseFlatsAndSharps {|stringNote|
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
			^stringNote.asInt
		});
	}
	
	*prAsPbindefLoop {|name,arr|
		^Pbindef(name,
			\degree,Pseq(arr[0],inf),
			\dur,Pseq(arr[1],inf)
		)	
	}
	
	*prAsPbindef {|name,arr,times=1|
		^Pbindef(name,
			\degree,Pseq(arr[0],times),
			\dur,Pseq(arr[1],times)
		)	
	}
	
}