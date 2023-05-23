# ComposeModalBottomSheet

ModalBottomSheet for Compose

## BaseUsage

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