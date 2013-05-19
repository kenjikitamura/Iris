package jp.rainbowdevil.iris;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IrisWindow extends Application{
	
	private static Logger log = LogManager.getLogger(IrisWindow.class); 
	
	@Override
	public void start(Stage stage) throws Exception {
		log.debug("ほげ");
		
        // ステージのタイトルを設定
        stage.setTitle("Iris");
        
        // ウインドウタイトルを非表示
        //stage.initStyle(StageStyle.UNDECORATED);
        
        Parent root = FXMLLoader.load(getClass().getResource("/MainWindow.fxml"));
        
        Scene scene = new Scene(root);
        
        // 
        scene.getStylesheets().add(IrisWindow.class.getResource("iris.css").toExternalForm());
        
        stage.setScene(scene);
        
        // ステージの表示
        stage.show();
	}
	
	public static void main(String[] args){
		launch(args);
	}

}
