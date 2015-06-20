# Reqs: A library for managing complex concurrent requests flow in Android 
Managing concurrent requests flow is difficult. And it's even more difficult combining with the complexity of Activity / Fragment life cycle. Reqs is created to:
- Provide a simple API to manage the order of the execution of the different requests and keep the responses with the ability to resume / pause / cancel a request flow. 
- Concurrent requests executed in parallel or in sequences can be chained together in a Reqs object and this make creating or modifying a request flow very easy.

## Communication
- Report issues in [GitHub Issues](https://github.com/mssjsg/reqs/issues)

# Sample usage
Examples usages can be found in the project located at Examples/Demo/.
```java
Reqs reqs2 = Reqs.create().then(new Request() {
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
    });
    reqs2.start();
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


