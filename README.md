# Reqs #

Reqs is a library for managing complex requests flow on Android in a simple way. The library is in pure Java now but I probably will need to use some Android stuff later to get the main thread. While this project almost feature completed (still missing a few) it still need more tests to stabalize it...
and I need more time to write more doc so that people would actually know how to use it. You can take a look at the Demo app in the repo to have a glimpse of what it can do now. For now, you still need to manage the threads yourself(e.g. create AsyncTasks) in the current version but Reqs can manage the order of the execution of the different requests and keep the responses.
I am working on the AsyncRequest class so that it will be able to do the threading too.

Feedbacks are welcome!