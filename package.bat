jmod --main-class=jlinkgui.Main --class-path build\classes\main --create build\libs\jlinkgui-1.0.0-SNAPSHOT.jmod
jar --create --file=build\libs\jlinkgui-1.0.0-SNAPSHOT.jar --main-class=jlinkgui.Main -C build\classes\main .
pause