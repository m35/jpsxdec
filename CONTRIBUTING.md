
# Feature requests

* If you find a game that has FMVs which jPSXdec can't find, please create an Issue for it
* If there's some feature you'd like jPSXdec to have, add an Issue for it

Unfortunately at this point I probably will never be able to implement any of these, but maybe someday someone can.

# Reporting bugs

* Reproduce the issue
* Locate the `debug00.log file` that was created
* Create an Issue on the Github project
* Fill out the form:
  * jPSXdec version
  * Operating system
  * Java runtime environment - you can get this by running `java -version`
  * The name of the PlayStation game, if applicable
  * Also the region code (e.g. SLUS-1234)
  * Description of what you were doing, what happened, and what should or should not happen
* ***Attach the `debug00.log` file*** (very important)

# Submitting changes

If you're interested in making changes to jPSXdec, let me know. I'd like to help if I can.
Email me at jpsxdec@gmail.com

For starters, read the `jPSXdec-design.md` file for the code styleguide and an overview of how
the program is designed.

## Dev environment

jPSXdec has its own Ant build script, but it doesn't do anything special.
For development, you could simply load up all the directories into the
IDE of your choice and work from there.

## Make the changes

Run the existing unit tests.

If there is core functionality you feel is important to never break,
write some unit tests for it.

Be sure to do some thorough manual testing.

For the final build, be sure to build using the Java 8 JDK to ensure no future
APIs are use. Run the Ant build script to ensure it builds without errors.

## License

All changes will need to be shared under one of these licenses:

* MIT License
* GNU Lesser General Public License

Or any other permissive copyleft license of your choice. If you don't care
how people use your code, I would recommend the simple MIT license.

I'll probably change the jPSXdec license to AGPL at some point. When I do, 
this will save me from having to ask permission from everyone that contributed.

## Commit message

Follow the basic Git commit message format suggestions:

* Separate subject from body with a blank line
* Limit the subject line to 50 characters (if practical)
* Capitalize the subject line
* Do not end the subject line with a period
* Use the imperative mood in the subject line
* Wrap the body at 72 characters
* Use the body to explain what, why, and even how

## Submit the pull request

It would be nice to have a Checkstyle format to check formatting, but
for now try to follow what you see done in the code. It's usually
pretty consistent. I may ask for some formatting changes.

All changes will be squash-merged.
