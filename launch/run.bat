@echo off
set JAVAFX_PATH=D:\Java\apache-maven-3.8.8\repository\org\openjfx
set JAVAFX_VERSION=21.0.1

set MODULE_PATH=%JAVAFX_PATH%\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-fxml\%JAVAFX_VERSION%\javafx-fxml-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-fxml\%JAVAFX_VERSION%\javafx-fxml-%JAVAFX_VERSION%.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-web\%JAVAFX_VERSION%\javafx-web-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-web\%JAVAFX_VERSION%\javafx-web-%JAVAFX_VERSION%.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-media\%JAVAFX_VERSION%\javafx-media-%JAVAFX_VERSION%-win.jar
set MODULE_PATH=%MODULE_PATH%;%JAVAFX_PATH%\javafx-media\%JAVAFX_VERSION%\javafx-media-%JAVAFX_VERSION%.jar

set CP=target/classes

java --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.media -cp "%CP%" com.review.agent.client.LaunchApplication
