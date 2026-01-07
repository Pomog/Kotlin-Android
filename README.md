# Kotlin-Android

Kotlin Android App Development: Firebase Firestore, Hilt &amp; Dagger, ROOM DB, ViewModel,
Navigation &amp; Clean Architecture
Android Jetpack Compose is a way of building modern android apps in android app development.

## ADB

```Powershall
Get-ChildItem env:
Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb kill-server
& $adb start-server
& $adb pair 192.168.1.138:3663
 
  Enter pairing code: 674903
 
Test-NetConnection 192.168.1.138 -Port 36639
& $adb devices -l
```

```java
    public static List<Integer> subarraySum(int[] nums, int target) {
        List<Integer> result = new ArrayList<>();
        int runningTotal;
        Map<Integer, Integer> indices = new HashMap<>();
        
        // runningTotal - index
        indices.put(0, -1);
    }
```
