# React native IO utils for Android

Network file upload and file download. As well as file/image/video picking utilities for android.

###Examples
---

```javascript
  
```

#Installation
---

Install the npm package [`react-native-io-utils`](https://www.npmjs.com/package/react-native-io-utils). Inside your React Native project, run ([example](https://github.com/Anthonyzou/react-native-io-utils/tree/master/example)):

```bash
npm install --save react-native-io-utils
```

In `android/settings.gradle` add the following lines

```
include :react-native-io-utils'
project(':react-native-io-utils').projectDir = file('../node_modules/react-native-io-utils/android')
```

---

In `android/app/build.gradle`, add a dependency to `':react-native-io-utils'`

```
dependencies {
    compile project(':react-native-io-utils')
}
```

Next, you need to change the `MainActivity` of your app to register `IOUtilsModule` :
```java
import com.rn.io.utils.IOUtilsModule; // add this import

public class MainActivity extends ReactActivity {
    //...

    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          new IOUtilsModule() // add this manager
      );
    }
```

---

TeamLockr io utils
Team Lockr io utils for react native

These are functions created by the TeamLockr Team created for the TeamLockr platform.

---
