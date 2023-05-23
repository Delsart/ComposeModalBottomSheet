# ComposeModalBottomSheet

ModalBottomSheet for Compose
https://github.com/Delsart/ComposeModalBottomSheet/assets/25666144/ff9ceef8-b30d-43fb-9d68-aa876b6ff383

## Base Usage

``` kotlin
BottomSheet(sheetContent = {/*TODO sheet content here */ }) {
    val bottomSheetState = LocalBottomSheetState.current
    val coroutineScope = rememberCoroutineScope()
    Button(onClick = {
        coroutineScope.launch {
            // show bottomSheet
            bottomSheetState.show()
        }
    }) {
        Text(text = "show")
    }
}
```

## Import

Step 1. Add it in your root build.gradle at the end of repositories:

``` groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Step 2. Add the dependency

``` groovy
dependencies {
	implementation 'com.github.Delsart:ComposeModalBottomSheet:v0.0.1'
}
``` 