# Reqs: A library for managing complex concurrent requests flow in Android 
Managing concurrent requests flow is difficult. And it's even more difficult combining with the complexity of Activity / Fragment life cycle. Reqs is created to:
- Provide a simple API to manage the order of the execution of the different requests and keep the responses with the ability to resume / pause / cancel a request flow. 
- Chain concurrent requests executed in parallel or in sequences together in a Reqs object and this make creating or modifying a request flow very easy.

##Why Reqs?
While there are already amazing libraries such as RXJava that can manage data flow very well, I think I need something tinier, simpler with *fewer* functions but just enough to get the job done. Also, I want to pause and resume a request flow to resolve some lifecycle issues in Android(especially in dealing with fragment in data callbacks), which *cannot* be easily done in other libraries(correct me if I am wrong). As a result, I created this library. The library is small (< 40 kb) but has been very useful in dealing with complex requests flow in my projects. This could be an alternative choice if you somehow don't want to use RXJava.

## Download
```groovy
repositories {
    jcenter()
}
```
```groovy
compile 'com.maksing:reqs:1.0.0'
```
## Communication
- Report issues in [GitHub Issues](https://github.com/mssjsg/reqs/issues)

## How to use
Please refer to the [GitHub Wiki](https://github.com/mssjsg/reqs/wiki) page and the demo project in /Examples/Demo
## Sample usage
Sample usages can be found in the project located at Examples/Demo/.
```java
private void doRequests() {
        Reqs.create().then(new Request() {
                @Override
                public void onCall(RequestSession session) {
                    doRequest("1", session);
                }
        
                @Override
                public void onNext(RequestSession session, Response response) {
                    super.onNext(session, response);
                    log(response.getData(Data.class).toString());
                }
            }.then(new Request() {
                @Override
                public void onCall(RequestSession session) {
                    doRequest("2", session);
                }
            }, new Request() {
                @Override
                public void onCall(RequestSession session) {
                    doRequest("3", session);
                }
            }).done(new Reqs.OnDoneListener() {
                @Override
                public void onSuccess(Reqs reqs, List<Response> responses) {
                    String resp = "";
        
                    List<Data> dataList = reqs.getDataList(Data.class);
        
                    for (Data data : dataList) {
                        resp = resp + data.str + "\n";
                    }
        
                    log("all requests done. responses:\n" + resp);
                }
        
                @Override
                public void onFailure(Response failedResponse) {
                    log("Requests Failed, cannot continue.");
                }
            }).start();
}

private void doRequest(final String data, final RequestSession requestSession) {

    log("Start request " + data);
    final Callback<Data, String> callback = new Callback<Data, String>() {
        @Override
        public void onSuccess(Data response) {
            requestSession.done(response);
        }

        @Override
        public void onFailure(String response) {
            requestSession.fail(response);
        }
    };

    handler.postDelayed(new Runnable() {
        @Override
        public void run() {
            if (TextUtils.isEmpty(data)) {
                callback.onFailure("Empty String!!");
            } else {
                callback.onSuccess(new Data(data));
            }
        }
    }, 2000);
}

```

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


