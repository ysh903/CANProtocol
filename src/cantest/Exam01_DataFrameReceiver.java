package cantest;

import java.io.BufferedInputStream;
import java.io.OutputStream;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class Exam01_DataFrameReceiver extends Application {

	TextArea textarea; // 받은 메세지 창
	Button connBtn; // COM port 연결 버튼

	// 사용할 COM 포트를 지정하기 위해서 필요
	private CommPortIdentifier portIdentifier;

	// 만약 COM 포트를 사용할 수 있고 해당 포트를 open하면 COM포트 객체를 획득
	private CommPort commPort;
	// COM 포트는 종류가 2가지 (직렬/병렬)
	// CAN 통신은 Serial 통신. 따라서 COM 포트의 타입을 알아내서 casting 시킴

	private SerialPort serialPort;
	// port객체로부터 Stream을

	private BufferedInputStream bis;

	private OutputStream out;

	// inner class 형식으로 event처리 listener class 작성
	class MyPortListener implements SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent event) {
			// Serial Port에서 Event가 발생하면 호출

			if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				byte[] readBuffer = new byte[128];
				try {
					while (bis.available() > 0) {
						bis.read(readBuffer);
					}
					String result = new String(readBuffer);
					printMsg("받은 메세지는 : " + result);

				} catch (Exception e) {
					System.out.println(e);
				}
			}

		}

	}

	private void printMsg(String msg) {
		Platform.runLater(() -> {
			textarea.appendText(msg + "\n");
		});
	}

	private void connectPort(String portName) {
		// portName을 이용해 Port에 접근해서 객체를 생성함.
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
			printMsg(portName + "에 연결을 시도합니다");

			if (portIdentifier.isCurrentlyOwned()) {
				printMsg(portName + "가 다른 프로그램에 의해서 사용되고 있음!");

			} else {
				// 포트가 존재하고 내가 사용할 수 있음.
				// 포트를 열고 포트 객체를 획득
				// 첫번째 인자는 포트를 여는 프로그램의 이름(문자열)
				// 두 번째 인자는 포트를 열때 기다릴 수 있는 시간(ms)
				commPort = portIdentifier.open("MyApp", 5000);
				// 포트 객체를 얻은 후 이 포트객체가 직렬/병렬인지를 확인한 후
				// type casting
				if (commPort instanceof SerialPort) {
					// Serial Port 객체를 얻어낼 수 있음!!
					serialPort = (SerialPort) commPort;
					// Serial Port에 대한 설정을 해야 함!
					serialPort.setSerialPortParams(38400, // Serial Port 통신속도
							SerialPort.DATABITS_8, // 데이터 비트
							SerialPort.STOPBITS_1, // stop bit 설정
							SerialPort.PARITY_NONE); // paraty bit 안씀
					// Serial Port를 Open하고 설정까지 잡아놓은 상태
					// 나에게 들어오는 Data Frame을 받아들일 수 있는 상태
					// Data Frame이 전달되는 것을 감지하기 위해서
					// Event처리 기법 이용
					// 데이터가 들어오는 것을 감지하고 처리하는 Listener 객체 필요
					// Listener 객체를 만들어서 Port에 리스터로 등록
					// Listener 개체를 만들기 위한 class가 있어야 함.
					serialPort.addEventListener(new MyPortListener());
					serialPort.notifyOnDataAvailable(true);
					printMsg(portName + "에 리스너가 등록되었어요!");
					// 입출력을 하기 위해서 Stream을 열면 됨!
					bis = new BufferedInputStream(serialPort.getInputStream());
					out = serialPort.getOutputStream();
					//CAN 데이터 수신 허용 설정
					// 프로토콜을 이용해서 정해진 형식대로 문자열을 만들어서
					// out stream을 통해서 출력
					
					String msg = (":G11A9\r"); // CANPro의 CAN 데이터 수신 동작을 시작
					try {
						byte[] inputData = msg.getBytes();
						out.write(inputData);
						printMsg(portName+"가 수신을 시작합니다.");
					} catch (Exception e) {
						System.out.println(e);
					}
					
					
					
				}

			}

		} catch (Exception e) {
			System.out.println(e);
		}

	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		BorderPane root = new BorderPane();
		root.setPrefSize(700, 500);

		// Component 생성해서 BorderPane에 부착
		textarea = new TextArea();
		root.setCenter(textarea);

		connBtn = new Button("COM 포트 연결");
		connBtn.setPrefSize(250, 50);
		connBtn.setOnAction(t -> {
			// 버튼에서 Action이 발생(클릭) 했을 때 호출!
			String portName = "COM4";
			// 포트 접속
			connectPort(portName);

		});

		FlowPane flowpane = new FlowPane();
		flowpane.setPrefSize(700, 50);
		flowpane.getChildren().add(connBtn);
		root.setBottom(flowpane);

		// Scene객체 필요
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("CAN Data Frame Receiver 예제");
		primaryStage.show();

	}

	public static void main(String[] args) {
		launch(); // start method 시작

	}

}
