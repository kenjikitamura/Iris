package jp.rainbowdevil.iris;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jp.rainbowdevil.iris.ui.WindowResizeButton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IrisWindow extends Application{
	
	private static Logger log = LogManager.getLogger(IrisWindow.class);
	
	
	@Override
	public void start(Stage stage) throws Exception {
		log.debug("ほげ");
		
        // ステージのタイトルを設定
        stage.setTitle("Iris");
        
        String osName = System.getProperty("os.name");
        if ( osName != null && osName.contains("Windows")){
        	stage.initStyle(StageStyle.DECORATED);
        }else{
        	// ウインドウタイトルを非表示
        	stage.initStyle(StageStyle.UNDECORATED);
        }
        
        //Parent root = FXMLLoader.load(getClass().getResource("/MainWindow.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
        final AnchorPane root = (AnchorPane) loader.load();
        IrisController controller = (IrisController)loader.getController();
        controller.setStage(stage);
        
        final WindowResizeButton windowResizeButton = new WindowResizeButton(stage, 100,100);
		windowResizeButton.setManaged(false);
		
		
		AnchorPane root2 = new AnchorPane() {
            @Override protected void layoutChildren() {
                super.layoutChildren();
                windowResizeButton.autosize();
                windowResizeButton.setLayoutX(getWidth() - windowResizeButton.getLayoutBounds().getWidth());
                windowResizeButton.setLayoutY(getHeight() - windowResizeButton.getLayoutBounds().getHeight());
            }
        };
        
        root2.getChildren().add(root);
        log.debug("root="+root.getClass().getName());
        AnchorPane.setTopAnchor(root, (double) 0);
        AnchorPane.setBottomAnchor(root, (double) 0);
        AnchorPane.setLeftAnchor(root, (double) 0);
        AnchorPane.setRightAnchor(root, (double) 0);
        
        root2.getChildren().add(windowResizeButton);
        Scene scene = new Scene(root2);
        
        
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
