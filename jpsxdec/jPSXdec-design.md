
# The jPSXdec design document

This documents the design of jPSXdec as of version 2.0.

Please also refer to `PlayStation1_STR_format.txt`
to get a bigger perspective on how/why jPSXdec is designed.

# Development environment

An Ant build script is used for official builds, regardless of IDE used.
But for developing, it's easier to ignore the Ant script and simply import all the code into a basic
Java project in the IDE of your choice.

However several of the UI forms were designed using Netbeans forms designer.
Netbeans would be required to modify those forms.

# Code styleguide

It would be nice to have a Checkstyle for formatting details, but egad is that a tedious process to make one

* Make as many things as immutable as possible.
* Use `final` on all fields if at all possible.
    * If it's not possible, consider making a new inner class that CAN use final fields.
* Code and comments are written to mostly minimize vertical size,
  so more lines of content can be visible on screen.
* Once lines are a little more than 80 characters long, consider looking for a place to split it (if you feel like it).
* Put the opening brace on the line that starts a block, unless you need to
  wrap the line, then put the brace on its own line.
* Align wrapped lines with the prior opening section.
* Wrapped lines should usually be indented at least 8 characters.
* Interface classes usually begin with `I`.
* Use `@NonNull` and `@CheckForNull` for every argument, return type, and field that is an object.
  A few places where that may not be necessary:
  - When it is a `final` field that is initialized at declaration.
  - When the possibility of a variable being null is too complicated, so the flag would just be a useless warning (should be very rare).
  - `toString()` method return type
  - Parameters for an empty method implementing an interface
* Keep the length of files less than 1000 lines-of-code (including header), but also more than 50 (excluding header) (i.e. put tiny classes in related files if it makes sense)
* Try to design the code such that there is only one reasonable way to do something
* `@Override` can be optional for inline anonymous classes
* Never use the Java 8 generic diamond operator. Always declare the generic type.
* Never use Java 8 lambdas
  - The only exception may be `Supplier` and `Consumer` classes, but only if the return type is unambiguous, and always declare the parameter names and types. The body must be 2 lines or less. Otherwise put the logic in a separate function.
* Minimize the use of Java 8 streams, and only for the simplest of chains. Never use `.forEach()`.
* Usually no wildcard imports, but optional when many static members are imported
* Imports are in alphabetical order with static imports at the end
* Try to add javadoc for non-trivial functions, and always keep the docs up-to-date with the code
* Per the javadoc tool, the first sentence (ending with a period) will appear in the summarized description. Not necessary, but if convenient, put the basic description in the first sentence.
* javadocs don't need to be formatted with HTML. In most cases it's better not to and simply format it in the code.
* Put final and non-null fields first
* Keep fields and constructor arguments in the same order
* Favor initializing fields in-line when doable

## Naming conventions

* primitive types prefixes:
  * long=`lng`
  * int=`i`
  * short=`si`
  * byte=`b`
  * double=`dbl`
  * float=`flt`
  * boolean=`bln`
  * enum=`e` (this is optional)
* array prefix: `a`+`<type>` (e.g. `int[]` should start with `ai`)
* arrays of objects have `ao` prefix
* primitive type objects are `<primitive prefix>`+`o`. e.g. `io`, `blno`, `dblo` etc
* fields start with underscore `_`
* nested non-static class fields usually start with double underscore `__`


# Design

Here is the entire design, broken down by packages and key classes.
Stacks from bottom up.
```
+-------------------------------------------------------------------+
|                          jpsxdec.Main                             |
+--------------------------------+----------------------------------+
|              gui               |             cmdline              |
+--------------------------------+----------------------------------+
|                          other modules                            |
+--------------------------------+----------------------------------+
|                          modules.player                           |
+--------------------------------+----------------------------------+
|        modules.video           |        modules.audio             |
+--------------------------------+----------------------------------+
|                            indexing                               |
+-------------------------------------------------------------------+
|                            discitems                              |
+-------------------------------------------------------------------+
|                    modules.SectorClaimSystem                      |
+-------------------------------------------------------------------+
|                    modules.IIdentifiedSector                      |
+---------+-------------+----------+-----------+------------+-------+
|  adpcm  |  cdreaders  |  formats |  iso9660  |  psxvideo  |  tim  |
+---------+-------------+----------+-----------+------------+-------+
|                               i18n                                |
+--------------------------------+----------------------------------+
|        util.aviwriter          |         util.player              |
+--------------------------------+----------------------------------+
|                               util                                |
+-------------------------------------------------------------------+
|                          javax.annotation                         |
+-------------------------------------------------------------------+
|                         3rd party libraries                       |
+-------------------------------------------------------------------+
```
3rd party libraries are described at the end.

Checked exceptions are used as much as possible to ensure all possible
conditions are covered at compile time. I shouldn't need to explain how
objectively superior checked exceptions are to unchecked.


## `javax.annotation`

JSR-305 `@NonNull` and `@CheckForNull`. The best thing to happen to Java since generics.
Used in practically every file. All modern IDEs recognize these and highlight
possible `NullPointerException`s. NPE has rarely been seen with this project.

## util

Only depends on `javax.annotation` and 3rd party libraries.
`IO.java` is probably the most used.


### `util.aviwriter`

I only know of one other Java AVI writer that doesn't use the old JMF.
It's mostly perfect.


### `util.player`

A pretty darn good library for real-time audio/video playback.
The javadoc explains how it works.

See `modules.player` for how it's used in jPSXdec.


## `i18n`

I18N = Internationalization = translations

`ILocalizedMessage` is the root of I18N.

The idea is that ANY text displayed to the user MUST be an `ILocalizedMessage`.
Never use raw Strings. But all internal logs and internal exceptions are in
English.

Ideally, user error messages should consider what the user can do about the issue.
If the user can't do anything to fix an issue,
there's not much point in saying anything more than "data is corrupted".
If they can fix it, try to explain how.

Any exception that contains a `ILocalizedMessage` should have an error message
that is intended to be consumed by a user.

All localization uses the standard Java localization resource approach
with `.properties` files. However, each messages is accessed through the huge
`I.java` class. This helps ensure messages exist, and argument count and types
match the message.

`ILocalizedLogger` is the way localized messages are logged for a user.
You will find an `ILocalizedLogger` as a `@NonNull` parameter all over the code
because log messages can be written all over the place.
Maybe someday this could be changed to thread local storage.
Using user log files was the best way I could think to tell the user all
the issues that may occur when indexing or saving data (since there could be thousands).

`LoggedError` combines localized logging and throwing an exception.
It logs immediately and then throws the exception. Maybe not the cleanest design.


## Several core packages

These are independent of each other and contain building blocks for higher modules use.


### `cdreaders`

The source of how everything reads from a disc image. I've tried to abstract
it to allow for reading from any kind of disc-like data.

It's best to read sectors in a forward sequential way since it buffers a
lot of data ahead of time, though random access really isn't a big deal
given today's hardware.
Memory mapped files were considered, but their primary
benefit is writing do disk. Reading is still faster with standard Java
`InputStream`.

There were plans to read
directly from the CD drive like older programs did, but that is notoriously
difficult to do on every platform.

### `psxvideo`

See `PlayStation1_STR_format.txt` to make some sense of how it all works.
That should cover the `bitstreams` and `mdec` and `idct` sub-packages.

All the pieces are made to be interchangeable.
The most brilliant piece of the video decoding and encoding design is the `MdecInputStream`.
It has provided a beautiful interface for all video processing to interact with each other.

If you have a frame bitstream, a quick way to convert it to an RGB image is like this.
```java
BufferedImage bitstreamToImage(byte[] bitstream, int imagePixelWidth, int imagePixelHeight) {
    BitStreamUncompressor bsUncompressor = BitStreamUncompressor.identifyUncompressor(bitstream);
    PsxMdecIDCT_double idct = new PsxMdecIDCT_double();
    MdecDecoder decoder = new MdecDecoder_double(idct, imagePixelWidth, imagePixelHeight);
    decoder.decode(bsUncompressor);
    RgbIntImage rgbImage = new RgbIntImage(imagePixelWidth, imagePixelHeight);
    decoder.readDecodedRgb(imagePixelWidth, imagePixelHeight, rgbImage.getData());
    return rgbImage.toBufferedImage();
}
```
I've used this block of code several times during development.


#### `psxvideo.bitstreams`

Holds the 2 "standard" bitstream formats found in the wild:

* STR v2 (v1 frames might as well just be v2 frames)
* STR v3

But also includes IKI and Serial Experiments Lain. Lain should probably be moved into its own module.

These 4 were easy to write bitstream compressors (encoders) for.

Other games had their own bitstream variations.
Unfortunately some games managed the bitstreams very differently, like
Road Rash and Aconcagua. Encoders for these would not be so easy.


#### `psxvideo.mdec.tojpeg`

Brilliantly translates a `MdecInputStream` into a JPEG image.
I have not seen anything that comes even close to this level of awesomeness.


#### `encode`

Tools for encoding a YCbCr image to PlayStation MDEC codes.
The YCbCr image should use the correct color range as specified in
`PlayStation1_STR_format.txt`.


### `tim`

Support for the standard PlayStation TIM image format.


### `adpcm`

ADPCM stands for "adaptive differential pulse-code modulation."
The PlayStation supports two, mostly identical, versions of ADPCM audio.
This package contains encoders and decoders for both formats.

See the mind-blowingly epic "Nocash PSX Playstation Specifications" for a description of these two formats.


### `iso9660`

Standard ISO 9660 filesystem support. See the ISO 9660 spec to help make sense of it.


### `formats`

A handful of image and audio format support that never ended up anywhere else.


### `discitems`

* `DiscItem`
* `DiscItemSaverBuilder`
* `DiscItemSaverBuilderGui`

"Disc items" identify something interesting on a disc that can be extracted
and maybe replaced. To extract the content, a `DiscItem` can generate a
`DiscItemSaverBuilder` that lets you set options (build) on how the content
should be extracted. A `DiscItemSaverBuilderGui` can also be generated which
is a `JPanel` with interactive options. The intent here was to let
`DiscItemSaverBuilder` be the model and `DiscItemSaverBuilderGui` be the
view/controller. `DiscItemSaverBuilder` also accepts command-line arguments
for setting the options. This design has worked pretty well.

Every disc item can be serialized and de-serialized as a string.
The string is saved/read from an index file.


### `indexing`

* `DiscIndex`
* `DiscIndexer`

A "disc index" maintains all the identified `DiscItem`s on a disc.

When indexing, `DiscIndexer`s are registered with a `DiscIndex` instance.
`DiscIndexer`s then register a listener to `ISectorClaimer`s in a
`SectorClaimSystem` (see below). As the sectors are read, the indexers build `DiscItem`s
and add them to the index.

The disc index is organized as a list, and also a tree.
Disc items are ordered sequentially by where they appear on the disc with a unique sequential id number. Disc items can hold child disc items. At the top of the tree is usually
the ISO 9660 files, with disc items that exist the file as child items.
The children are given an additional child index number in the order they appear
in the parent item.

Disc items can be accessed using the unique index number, or using the full path to the tree node.


# Modules

I hope the names are pretty self-explanatory.
They're all mostly independent of each other.
In theory you could remove a module without any issue.

For a time I didn't break the support for each game into a separate module.
Instead, I broke it up by sectors, disc items, and indexers.
That wasn't bad, but for some reason I went back to modules again.

At the root of the modules are some fundamental components:


## `modules.IIdentifiedSector`

Identifying sectors is the core building block behind identifying data on the disc.
Identified CD sectors get wrapped in an identified sector, derived from the `IIdentifiedSector` interface.

Identified sectors come with a "probability" that it is a correctly identified sector.
I added this at the very beginning, following the pattern ffmpeg uses.
In the last 10+ years, I've never needed anything besides 0% or 100%.
So that could simply be replaced with true/false.

To support the probability, you'll see the use of `isSuperInvalidElseReset()`.
It's kinda a weird way I used to propagate the probability that a sector
matches from a super class to a child class.

```java
super(cdSector);
if (isSuperInvalidElseReset()) return;
```

In the constructor, call `super()`, then in the next line check if
the super class recognizes the sector. If the super class does not
recognize the sector, then immediately return. If it does,
reset the probability back to 0 so that this subclass can do its own
validation. If the sector fails a validation, simply return.
Otherwise, at the end of the constructor set the probability.
```java
setProbability(100);
```
If there is yet another child class, it should do the same thing, and so on.

## `modules.SectorClaimSystem`

If jPSXdec only needed to look for 1 thing on the entire disc, it would be
pretty simple. Read some data and see if it matches what we're looking for.
If it matches, collect the data and we're done, otherwise continue the search.

That process gets painfully inconvenient when looking for multiple things
on the disc at the same time. Some data may even looks so closely to other data
that there could be false positive identification. So by design some identifiers
needs to take priority over other identifiers.

One option would be to scan the entire disc multiple times, each time searching
for something different. Not a bad idea, but still, for each pass, you need
to remember what the previous passes found to avoid false positives when
two search items look similar.

After a few iterations, I ended up with the "sector claim system"
(never figured out a better name).

Each "claimer" (implements `ISectorClaimer`) is registered with a
`SectorClaimSystem` instance.

The claim system then takes each CD sector and passes it to each claimer, one by one.
Once a claimer identifies a sector, it 'claims' it. Claimers down the chain
can no longer claim the sector, but it offers the ability for one claimer
to be aware of another claimer's sectors and use it to help identify its own data.

Simple enough, unfortunately some claimers need to look ahead to determine
if the current sector is a match.

I call this contextual identification because it needs to know the surrounding
sectors to know if the current sector is a match.
This is similar to defining a grammar that is not "context free".
In this case the lookahead amount is unknown since any game could do it
differently, so is theoretically infinite (in practice you'd want to keep the
lookahead at a reasonable amount since it buffers the sectors using memoization).
Grammars have the advantage of BNF and Yacc to generate optimized matchers
(I found one library that could do this for binary data as well, which could be an option).
This problem could also be solved using continuations, which I dabbled in.

On top of all that, before a claimer can even peek ahead at the
next sector, all the previous claimers need to look at it first.

So in the end I went with making the sector identification process recursive.
When one claimer needs the next sector, it pulls the sector from the disc
and up through all the other claimers until it reaches the peek request.

After a sector has passed through all the claimers and is hopefully identified,
it is then sent to any registered listeners. The listener processes the
identified sectors and forwards it to anything listening to that listener,
all the way until it ends up in a file or displayed on the GUI.

### `modules.video.*`

Some modules rely on `modules.video.*` since that holds so much reusable logic.

You'll see the use of "presentation sector" among the code.
This is like a presentation time: the moment in time when a frame should appear,
or a portion of audio should be played.
Except instead of a timestamp, it is a sector number.
This worked well for presentation timing because you may not always know
the disc speed, which could either be 75 sectors/second or 150 sectors/second.
So using a sector number keeps the timing independent of the disc speed.
At 150 sectors/second, sector 30 would be at 0.2 seconds, but at
75 sectors/second it would be at 0.4 seconds.
When it comes to actually saving the video, or playing it in real time,
at that point the disc speed could be chosen (which may need
to be done by the user).


#### `modules.video.framenumber`

Frame numbers proved to be shockingly complicated.
I felt it was important to identify frame numbers 3 different ways:

* Frame index starting at 0
* Frame number as found in the frame header meta-data
* The sector the frame starts on

The manual covers some details about how they are used.

For each, it is necessary to know the maximum value so when image sequences
are generated, the file names are padded with the right amount of zeroes.
It gets trickier because the frame number found in the header meta-data
often times repeats for various reasons. So for repeated frames I
tacked on a decimal indicating the index of the duplicate. Of course
that means we also need to know the maximum duplicate count so we
can pad those numbers with zeroes as well. Same goes with the sector number.
In some cases, if a frame is small enough, multiple frames can begin in
the same sector. So need track that duplicate count as well.

TBH I even sometimes get confused by this code, but it really is about as
simple as it can be.


#### `modules.video.save`

The `save` package centers around the `VDP` which means
"video decoding/decoder pipeline" (since writing it out would be a pain everywhere).
The process of converting a bitstream to a file on the computer involves
lots of steps, each having their own options to choose.
The VDP creates a pluggable pipeline that pipes the data through
all the steps to the final result. The `AutowireVDP` is also there to help
with building the pipeline.

There also some important classes to keep the audio and/or videos in sync.

* `AudioSync`
* `VideoSync`
* `AudioVideoSync`

These determine if any silent audio needs to be written to keep audio timing correct,
or if any duplicate frames need to be written to keep the frame timing correct
(sort of a poor man's variable frame rate implementation).


#### Video types

After years of seeing all the different ways games stored video data, they all
boiled down to two types:

 * those based on sectors
 * those based data packets


##### Sector based videos `sectorbased`

This is the traditional video style. It uses the PlayStation and 'Green Book'
standard method of streaming 'real time' sector data using a single speed
75 sectors/second or double speed 150 sectors/second. I've only seen 1
case where a game streamed at 75 sectors/second. Almost all will run at
150 sectors/second.

Sectors are read at that rate and frames are combined (demuxed), then displayed
at the moment all the frame data has been read.

The audio is contained in intermingled audio sectors, predominately using XA
audio also defined in the Green Book standard.

See the `PlayStation1_STR_format.txt` document for some more details.


###### Sector based frame rate detection `sectorbased.fps`

This is the one thing that has kept jPSXdec from being "feature complete".
By that I mean, when jPSXdec has all the necessary core components to support
any kind of video.

Here lies the most complicated part of the entire jPSXdec codebase.
So complicated in fact that I never found a solution to it after trying
several different approaches.

Identifying the frame rate of sector based videos is hard. Crazy hard.
This package only handles the 2 easiest methods: frames that have a constant
sector count, and a handful of videos that I added special handling for.
Otherwise it falls back to just dividing the number of frames by the duration of the movie,
which is often Good Enough. However I wanted it to be better than that,
and also support videos with variable frame rates, which barf when
the frame rate = number of frames / duration.

Why is it so hard to determine the frame rate? See Appendix 2 for the explanation.


##### Packet based videos `packetbased`

Packet based videos have no respect for the sector they are in.
They do not use the PlayStation's built in 'real-time' streaming.
The sectors don't define when the frames should appear.
Instead the audio and video frames are packed tightly together.
Each packet is a video frame, or a portion of SPU ADPCM encoded audio which may be
stereo or mono, or maybe some other metadata the game uses.

There's no standard way to store packet based videos, so every game did their
own thing. The game handles presenting the frames and audio itself, with whatever
frame rate or audio rate it wants. You just have to dig into each one and see what they did.


#### `modules.video.replace`

Originally the goal was to be able to replace any video in any game.
In recent years however I see that replacing frames really only
works for sector based videos. This is because the space available
for each frame in sector based videos is well defined.
Also many frames have empty sectors to pad the frame presentation timing,
so it allows replacement frames to grow in size.

If we take the same approach for packet based videos, it proves
to be rather unhelpful. Since packet based videos are packed
tightly together, there is no extra room for replaced video
frames to grow in size. So to realistically replace packet
based video frames would require unpacking the entire
video, change the frame(s) and repack them back together,
updating sizes and offsets along the way.

The only packet based video that I added replacement support for
is Crusader: No Remorse. It suffers from the same issue.
I added it before I realized it isn't very useful.
That replacement code really should be removed.


## `gui` and `cmdline`

Here is where it all comes together and provides the external interface to most
of the functionality below it.

The GUI was written in Swing, long before JavaFX existed. I know recently
there's been some confusion around if Java would drop Swing or JavaFX support
at some point. Whatever the case, the Swing UI will remain unless there is
some pressing need to convert to JavaFX.

I admit the UI design was inspired by PsxMC.


## `jpsxdec.Main`

Where the executable starts.


# Third-party libraries

Given the license that jPSXdec uses, all source code must be provided in
releases. So I was reluctant to include very large 3rd party libraries.
It turned out to not be a problem because jPSXdec does so many unique things
that few libraries proved to be worth using. Even Apache Commons didn't
offer much.


## `JXTreeTable` (`org.jdesktop`)

Reliable tree-tables in Java have a long history of disappointment, so
finding a robust implementation was important to me.

SwingLabs Swingx library provided enhanced version of many Swing components,
and some components not available in the standard SDK, such as the `JXTreeTable`.

Since so much effort was put into making the `JXTreeTable` a reliable and robust
implementation of the tree-table (look at the code and all the work-arounds
they added), I had hoped it would be practically issue-free.
So I carved out just the code needed for the `JXTreeTable` component
and then back-ported to Java 5 (it was written for Java 6+).

Unfortunately I've found even this `JXTreeTable` behaves somewhat awkwardly.


## `com.pdfjet`

I wanted a way to visualize the entire disc, with every sector type having
its own color, and every disc item spanning all the sectors it covers.
It's been useful several times. I couldn't think of a better way to do this
than to generate a pdf. html wasn't an option because the generated file
would be up to 100mb in size, which browsers aren't so great at handling.
Using an image would create one that is 400,000 pixels high--also unmanageable.
Even the generated PDF is large, generating a page with the maximum size
a pdf can handle.


## `com.mortennobel.imagescaling`

Used for scaling the Chroma (Cb/Cr) components of decoded video frames.


## ArgParser `argparser`

Early in jPSXdec development I didn't want to write my own
comandline parameter parsing and handling. After looking at several available, I settled
on ArgParser. After several years I had to do a thorough re-design
of the command-line options to expose all the many features available.
ArgParser was still used, but its usefulness diminished.
In the last few years I see that a custom argument parser would make
more sense, simplify the code, and probably reduce the amount of code.


## `JDirectoryChooser` `com.l2fprod`

I needed a way to choose the output directory, and ran across `JDirectoryChooser`.
It... works... mostly. At the time it had a nasty bug that would take
minutes to create the directory tree, which I made a hacky fix for.
jPSXdec really deserves something better, but it's never been a priority,
and I don't know if there is even anything better.


## `ParagraphLayout` `com.jhlabs.awt`

The Java look & feel design guidelines recommend having widgets laid out in rows,
and in two columns, with a label left-aligned, next to the widget which should also be left-aligned,
like jPSXdec has in its option panels. Yet the JDK doesn't have a layout
style that makes this easy. Hunting for a solution, I ran across this
`ParagraphLayout`, and it has been absolutely brilliant.


# Memory use

jPSXdec reference graph is fairly simple.

The root for a disc in memory is `ICdSectorReader` and `DiscIndex`.
Once a disc is loaded, no memory is allocated or released.
And once the `CdFileSectorReader` is closed and discarded, everything
associated with a disc is freed
(assuming nothing else is holding onto some exposed internal reference).

The UI is also pretty simple. Most is allocated once and never freed.
The real-time audio/video player is created and freed on every view.

The only memory growth that could be noticed would come from the
`DiscItemSaverBuilder` used to save every `DiscItem`. Initially each `DiscItems`'s
`DiscItemSaverBuilder` doesn't exist and are only created when the disc item
is selected in the GUI. But that should be minimal. And once the disc is closed,
those are all freed as well.

# Appendix 1:  Adding support for a new game

Other than the frame-rate detection limitations, jPSXdec has all the features to
support what any game might do. The rest of jPSXdec's existence will be
to add support for every game that does something unique (among other things).
I would guesstimate that about 10% of all PlayStation 1 games did something unique.
Since there were 1300 games released, that would be 130 games -- not a small feat.

If you discover a game that has FMVs, but jPSXdec doesn't find them, the game
is doing something unique. Here are the steps you could take to
add support for that new game.


## 1. Look at the sectors

If you find XA audio sectors, there will undoubtedly be video running in parallel.
Look for those non-XA sectors. Look at the first 32 bytes.
Do you see any patterns?
The `PlayStation1_STR_format.txt` shows how normal video sectors look.
Do you see anything similar?
Often there is a value indicating the frame number.
There's a value indicating how many 'chunks' or sectors this frame consists of,
along with the chunk number that the sector represents.
Sometimes there are values for the width and height.
Usually there's some 'magic' value that is found in every header.

If you see this pattern, make sure to look at EVERY sector that matches this
pattern for minimum and maximum values, along with any other variation.
That's to help make sure the sectors will be properly identified.

When you know what all the possible values are, you'll want to create a new
sector type. Since this kind of sector format is quite similar to normal
STR video sectors, you can probably just add it under `jpsxdec.modules.strvideo`.
You should be able to extend `SectorStrVideo` or `SectorAbstractVideo`.

In the constructor, follow the pattern used in other identified sectors.
Add fields for each value in the header, then read the values out of
the `CdSector` and check if they are correct. There's some existing helper functions
and classes you could use that handle a lot of the common cases.

Finally add the sector to `VideoSectorIdentifier` so it is included in the identification.

Just adding support for the sector may not be enough.
Check that jPSXdec is properly identifying the
frame image type. If it can't, you will see it in the logs.
The game may be using an unknown frame type.
If that is the case, check the section below.


## 2. Don't see any XA audio?

The ISO9660 files may suggest which files contain audio/video data.
Open those up in a hex editor and start looking for patterns.

Some things to look for:

* Frame bitstream headers. Check the `PlayStation1_STR_format.txt` for what a video bitstream header usually looks like.
You're in luck if you can locate several of them in a row.
When you find one, extract the header along with a bunch of bytes after it (since you may not know exactly where the frame data ends).
Use the jPSXdec command-line to decode a single frame (see manual) and see what happens.
If you're in luck you'll get part of a decoded frame.

* Black frames. A frame that is all black, or any black in a frame, will consist of the same 18 bytes repeated over and over.
The exact bytes will differ depending on the bit alignment.
If you can find the header of that frame, you can extract the frame and try decoding it.

* SPU audio. A SPU audio sound unit is 16 bytes, with the 2nd byte being 0. If you find a long sequence of these, it may be SPU audio.
Unfortunately jPSXdec doesn't offer any direct way to decode raw SPU audio. So to test this you'll need to write a small program
to read the suspected SPU data and send it to an `SpuAdpcmDecoder` and then write the decoded audio to a wav file using the standard JDK audio api.
For the audio sample rate, you can try using some of the standard XA ones.
Hopefully you won't just hear noise.

If none of these help, or even if they do, you may still not know enough to add support for it.

The first thing I do next is open the game in the pSX emulator, pull up the
debugger, and set a breakpoint for when the MDEC chip reads data.
When the breakpoint is hit, you will know where the game had written the MDEC codes
that had been converted from the original frame bitstream data.
Somewhere in memory will still be that bitstream
data that was read off the disc. You may also want to track what sectors
had just been read, because the frame data probably came from them.

If you didn't know where the frame data was on the disc, that should tell you.
Then the earlier steps may be usable.


## 3. Frame type is not recognized

If you know exactly where the frame data exists on the disc, but it's
still not obvious how the game is converting that data into MDEC codes,
then you better start learning R3000 MIPS assembly. That will be the only
way to know what the game is doing.

As mentioned, pSX has a debugger, and so does the Nocash no$psx emulator.
pSX has the advantage of DMA breakpoints. Radare2 I believe has PlayStation
support, and Ghidra does too. But static code decompiling often
isn't helpful in PlayStation games because some games dynamically load and unload
code from the disc into memory (FF7 mini-games are an example of this).

As a side note, one other tool I've used is a quick hack out of pcsx-r to log
what sectors are read, and all MDEC data being decoded. This is basically like
what I described you can do in the pSX emulator, it's just handy to have
a log of it. Finding where the MDEC decoded data is stored in RAM is easy.
But if there was some way to know what data was read prior to writing
that MDEC data, that could pinpoint where in RAM the raw frame data lies
that had been read from the disc. And if you could go one more step and
track which sector was read into that block of memory, it would be a pretty
handy way to quickly connect all the dots.


## Example

*Here is my experience reverse engineering the Aconcagua video format.
Hopefully you'll never encounter something this bad.*

I found where the video data was on the disc, but jPSXdec didn't recognize it.
Located the function in the code that decoded it, but it was hard to follow.
So I loaded the game into pSX, and had to wait for the video to start
because it dynamically loaded the video player code from the disc.
Then I had pSX dump all the assembly code of the function. I spent a lot of
time trying to walk through the code myself, but it still made no sense.

So I wrote a program to translate R3000 MIPS assembly into equivalent C code,
along with all the registers and RAM. It was a mini R3000 emulator, but in C that
could be compiled to a program. Cleaning that up, I was left with the function
I needed to understand.

To ensure it worked, I ran the game to the start of the function and paused
there. I dumped all of the RAM, and captured all the values of all the registers.
Then I let the game run until it hit the end of the function, and paused there.
Again I dumped all the RAM and grab all the register values.
In my little C emulator, I would initialize all the registers to the same ones
at the start of the function, and load all the RAM into memory.
Then I would run the function in my emulator until it ended. Finally I would
check that all the registers match, and all the RAM was (nearly) identical
to what the game had at the end of the function (nearly because
other DMA operations were changing unrelated parts of RAM). It actually worked,
and pretty well. I did this a few more times for more test cases.

Once I was confident that everything was working right, I began
refactoring that translated C code. Manual decompiling at its
finest. After some refactoring, I would run the tests again to make sure
I didn't make a mistake that broke the logic.

Since this decoding function was called serval times for each frame, I grew
worried that this one function didn't contain the entire decoding logic. So I
broadened my search to the function that called it, and all the other functions that it
called, and so on. That got real overwhelming real quick. But then I took a
closer look at the RAM between each time the function was called. I saw that
the pointer to the source data consistently moved forward in an incremental way,
that the decoded output pointer and data also never changed outside of
this one function. So I could go back and focus just on this function.

After a lot of refactoring, the patterns started to emerge. The game used
its own bizarre method to encode its video frames. Finally I translated
that logic in to Java code and put a giant "what even..." in the javadoc.


# Appendix 2: *Precise* sector based frame rate detection

The goal has always been to find the *exact* frame rate used to encode the video.
Approximations are pretty easy, but jPSXdec should be able to do better than that.

Sector based videos arrange the frames on the disc so they end at the very moment
they need to appear on the screen. This means many frames are padded to
make sure they don't appear too early. The idea is simple enough.
A video with a constant frame rate should mean that every frame uses
the same number of sectors.

Since the disc can spin at most 150 sectors/second, you could make a video
that uses 1 frame per sector, and your video would play at 150 frames/second.
If each frame uses 2 sectors, you get 75 frames/second (I've seen one case
of this).

This means that the fastest frame rate is 150fps. The slowest frame rate could
maybe be 5 frames per second, which would mean each frame would be 30 sectors
long. That's probably not actually possible, but it will be somewhere in that
direction. For a given frames/second, you can calculate the sectors/frame by
dividing sectors/second (which we can assume is always 150) by the frames/second.

The most common frame rate is 15 fps. 150/15 = 10 sectors per frame.
Each frame needs to end on every 10th sector, which on its own,
should be easy to check.

Now to mix it up a bit. How about 20 fps? Well that would mean each
frame takes 7.5 sectors. Obviously that won't work, but we can evenly
alternate which frames will be 7 sectors long, and which ones will be 8
sectors long. But will that be [7, 8, 7, 8] or [8, 7, 8, 7]?

Next try 24 fps, which means 6.25 sector frames. The frame lengths will
alternate between 6 and 7 sectors long, but where will the frame with 7 sectors appear?
[6, 6, 6, 7], [6, 6, 7, 6], [6, 7, 6, 6] or [7, 6, 6, 6]?

Have you figured out an algorithm to reliably detect these cases, along with
all other consistent frame rates? Let's say you did. Here comes the next challenge.

Most videos also have an audio track. The audio track appears as XA
sectors intermingled in even distances among the frames. The XA sector
essentially steals one sector from the frame it lands on.
The worst case of this is when an XA sector steals a sector at the
start or end of the frame. Now it becomes unclear which frame it stole
the sector from: the end of the current one, or the start of the next one?

This isn't a huge issue for 15fps. Now you can't just check if the last
sector of the frame lands on every 10th sector. You have to check that
every frame ends on every 10th sector, or the 9th sector if the 10th sector
is XA audio. It gets stickier for 24fps since the 4 different ways you could
alternate between 6 and 7 sector long frames gets a little harder to detect.

You might think, "why not just brute force test for every whole-number frame rate in every
way the fractional frame sector lengths could be arranged?" Not a horrible idea.
Doing the math, and there are over 30 possible combinations just for the simple
cases where the fractional sectors/frame is 0.5, 0.25, or 0.75. All the other
fractional values are really ugly and would balloon the combinations well over 100.

So we might have a plan for whole-number frame rates, but what about fractional frame rates?
A video where each frame uses 7 sectors would be 21.43fps. So I guess we'll tack on
a way to check for videos that use whole-number sectors/second.

I don't even know where to start if neither the frames/sector or sectors/frame are
whole-numbers...

Now maybe, just maybe, we could find a way for all this to work.
But here comes the next challenger.


## NTSC

From what I've read, the PlayStation dev tool that creates videos would
let you choose the frame rate, but it also included one more option: adjust the
frame rate to use NTSC timing.

The NTSC frame rate is defined to be 30 divided by 1.001 = about 29.97 fps.
We can look at it like this: after playing 1000 frames at 30 fps,
you need to skip 1 frame to adjust the frame rate by 0.001.

Applying NTSC rate adjustment to PlayStation videos should theoretically work the same
way. After the video has played for 1000 sectors, there will need to be
1 frame that is made 1 sector longer.

So maybe we can salvage this. Just add this extra check on top of whatever
algorithm we've made thus far. Unfortunately, the way NTSC frame rate *should*
theoretically be implemented is not how the PlayStation development tool
actually implemented it.

In reality, a video with NTSC frame rate will play out like it is not NTSC up to the point when
one sector needs to be added at 1000 sectors. That sector is added as
expected, but after that, the number of sectors each frame uses becomes
erratic. Suddenly frames are no longer evenly laid out. Frames that should take
10 sectors are taking 11, or 9. This must have been a bug with the math
used in the PlayStation dev tools. So good luck trying to determine
the exact frame rate by looking at where each frame ends.

But we're not done yet. There's still one last pill left to swallow.


## Variable frame rates

Up to now we've only been talking about *constant* frame rates. The frame rates
may have been fractional, but still constant. So in the worst case, if all else
fails, we could still fall back to a less accurate frame rate by simply dividing
the number of frames by the duration of the video. I don't like it, because
jPSXdec should do better than that, but whatever.

Unfortunately there's no shortage of videos that have the dreaded *variable frame rate*.

We've seen that in constant frame rate videos, the number of sectors
a frame uses can fluctuate by at least +/- 3 sectors.
As we are trying to wrestle with these fluctuations, at what point do we notice
that these aren't just fluctuations, but full frame rate deviations?


### How to save variable frame rate videos

And finally, after all these challenges, if we are somehow able to really identify
that a video has a variable frame rate, what do we do then? Currently jPSXdec can only
save AVI files which require a constant frame rate.

You could do a bit of a hack and use a higher frame rate, and then duplicate
frames as necessary to give the illusion that the frame rate is changing.
AVI files actually have a clever hack that allows you to duplicate frames
like this, but without having to actually write the frame multiple times.
Unfortunately this hack is not compatible with all video players.
Nevertheless, it is still an option. Which leads to the next question of:
what constant frame rate do you use that will handle all the variable frame rate changes?

Since 150 fps is technically the fastest a video can play, we could create a
video that runs at 150 fps and duplicate frames as needed. This will give us
100% perfect accuracy in timing, but is pretty intense. Maybe we could back off to
75 fps, so at worst a frame might be half a frame off. If you're lucky,
you might find a better constant frame rate that minimizes the number of duplicate
frames but still keeps the timing close to perfect.

#### mkv support

It's nice to remember that converting from variable frame rate to constant
frame rate isn't a new problem. Programs have found ways to do that for years.
Since AVI doesn't support variable frame rates, I've already made progress
into adding .mkv export support. mkv support variable frame rates.
The plan is to make that the default and the only way to export videos
with variable frame rates.


# About the name and logo

## "jPSXdec"?

When choosing a name for this program, I wanted something unique
so it would be easy to search for.

### `j`

TBH I like the tradition of naming Java programs with the `j` prefix.
As a Java developer, I am also a Java power user.
When I know a program is Java based, I can make better use of it,
and hack it if needed.

### `PSX`

I know there's controversy around using PSX to describe the PS1.
I wasn't aware of that at the time, and several other tools used
PSX, so followed their lead.

### `dec`

This was originally intended to mean "decoding",
similar to the use of "dec" in the term "codec" (which stands
for "coding and decoding").

Since then, the "dec" could also mean

* *d*ecoding
* *e*ncoding
* *c*onverting

### How do you say it?

The most straight forward way to say it would be "jay-pee-ess-ex-deck",
so it's probably the best. I personally rarely refer to the program by name,
and often informally shorten it to just "jPSX" or "PlayStation converter"
when talking to others about it.

* Note: The name "jpsx" is already taken by the PS1 emulator written entirely in Java.

### Logo

The program icon and logo uses the PlayStation 2 style logo which may have
led people to believe jPSXdec supports PlayStation 2 media.
But jPSXdec only supports PlayStation 1 media.
I just couldn't think of how to create a logo based on the PlayStation 1 logo.
And I'm not very artistic.
