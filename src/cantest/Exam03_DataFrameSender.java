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

public class Exam03_DataFrameSender extends Application{
	TextArea textarea;	// 메시지 창(받은 메시지를 보여주는 역할)
	Button connBtn;		// 연결버튼(COM포트 연결버튼)
	
	// 사용할 COM 포트를 지정하기 위해서 필요
	private CommPortIdentifier portIdentifier;	

	// 만약 COM 포트를 사용 가능하고 해당 포트를 open하면 COM 포트 객체를 획득
	private CommPort commPort;
	
	// COM 포트는 종류가 2가지(Serial, Parallel 두가지 종류)
	// CAN 통신은 Serial 통신을 함. 따라서 COM 포트의 타입을 알아내서
	// type casting을 시킴
	private SerialPort serialPort;
	// Port 객체로부터 Stream을 얻어내서 입출력 가능
	private BufferedInputStream bis;
	private OutputStream out;
	
	// inner class형식으로 event처리 listener class를 작성
	class MyPortListener implements SerialPortEventListener{

		@Override
		public void serialEvent(SerialPortEvent event) {
			// Serial Port에서 Event가 발생하면 호출!
			if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				// port를 통해서 데이터가 들어왔다는 의미
				byte[] readBuffer = new byte[128];
				try {
					while(bis.available() > 0) { // bis에 stream이 존재하는가?
						bis.read(readBuffer);
					}
					String result = new String(readBuffer);
					printMsg("받은 메시지는 : " + result);
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}
	}
	
	private void printMsg(String msg) {
		Platform.runLater(()->{
			textarea.appendText(msg + "\n");
		});
	}
	public static void main(String[] args) {		
		launch();
	}
	private void connectPort(String portName) {
		// portName을 이용해 Port에 접근해서 객체를 생성
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
			printMsg(portName + "에 연결을 시도합니다.");
			
			if(portIdentifier.isCurrentlyOwned()) {
				printMsg(portName + "가 다른 프로그램에 의해서 사용되고 있어요!");
			} else {
				// 포트가 존재하고 내가 사용 가능!
				// 포트를 열고 포트 객체를 획득
				// 첫번째 인자는 포트를 여는 프로그램의 이름(문자열)
				// 두번째 인자는 포트를 열때 기다릴 수 있는 시간(밀리세컨드)
				commPort = portIdentifier.open("MyApp", 5000);
				// 포트 객체를 얻은 후 이 포트객체가 Serial인지 Parallel인지를
				// 확인한 후 적절하게 type casting
				if(commPort instanceof SerialPort) {
					// Serial Port 객체를 얻어낼 수 있다!
					serialPort = (SerialPort)commPort;
					// Serial Port에 대한 설정을 해야함!
					serialPort.setSerialPortParams(
							38400,	// Serial Port 통신 속도
							SerialPort.DATABITS_8,	// 데이터 비트
							serialPort.STOPBITS_1,	// stop bit 설정
							serialPort.PARITY_NONE);	// Paraty bit는 안씀

					// Serial Port를 Open하고 설정까지 잡아놓은 상태
					// 나에게 들어오는 Data Frame을 받아들일 수 있는 상태
					// Data Frame이 전달되는 것을 감지하기 위해서 Event처리 기법을 이용
					// 데이터가 들어오는 걸 감지하고 처리하는 Listener객체가 있어야 함
					// 이런 Listener객체를 만들어서Port에 리스너로 등록해주면 됨!
					// 당연히 Listener객체를 만들기 위한 class가 있어야 함!
					serialPort.addEventListener(new MyPortListener());
					serialPort.notifyOnDataAvailable(true);
					printMsg(portName + " 에 리스너가 등록되었습니다!");
					// 입출력을 하기 위해서 Stream을 열면 된다!
					bis = new BufferedInputStream(serialPort.getInputStream());
					out = serialPort.getOutputStream();
					// CAN 데이터 수신 허용 설정
					// 이 작업은 어떻게 해야 하나?
					// 프로토콜을 이용해서 정해진 형식대로 문자열을 마들어서
					// out stream을 통해서 출력
					String msg =":W28"+"00000002"+"4A414542454F4D21"+"4F"+"\r";	// CANPro 통신 프로토콜 메뉴얼 7p 참조
					//"명령어"+"아이디"+"내용"+"checksum"+"\r"
					
					System.out.println(getCheckSum("W28"+"00000002"+"1122330000000000"));
					
					try {
						byte[] inputData = msg.getBytes();
						out.write(inputData);
						printMsg(portName + "가 수신을 시작합니다.");
					} catch (Exception e) {
						System.out.println(e);
					}
				}
			}
		} catch (Exception e) {
			// 발생한 Exception을 처리하는 코드가 들어와야 함
			System.out.println(e);
		}
	}
	private String getCheckSum(String msg) {
		int checksum = 0;
		for (int i = 0 ; i <msg.length() ; i++) {
			checksum += msg.charAt(i);
		}
		checksum = checksum & 0xff;
		
		return Integer.toHexString(checksum).toUpperCase();
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane root = new BorderPane();
		root.setPrefSize(700, 500);

		textarea = new TextArea();
		root.setCenter(textarea);
		
		connBtn = new Button("COM 포트 연결");
		connBtn.setPrefSize(250, 50);
		connBtn.setOnAction(t->{
			// 버튼에서 Action이 발생했을 때 호출
			String portName = "COM4";	// 장치관리자에서 연결된 포트 번호 확인
			// 포트 접속
			connectPort(portName);
		});
		
		
		FlowPane flowpane = new FlowPane();
		flowpane.setPrefSize(700, 50);
		//flowpane에 버튼 추가
		flowpane.getChildren().add(connBtn);
		root.setBottom(flowpane);
		
		//Scene객체가 필요
		Scene scene = new Scene(root); 
		primaryStage.setScene(scene);
		primaryStage.setTitle("CAN Data Frame Receiver 예제");
		primaryStage.show();
	}
}