package jp.rainbowdevil.iris.preferences;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PreferencesController implements Initializable{
	
	private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(PreferencesController.class);
	
	private Stage stage;
	
	@FXML
	private TextField proxyAddressTextField;
	
	@FXML
	private TextField proxyPortTextField;
	
	@FXML
	private Button okButton;

	private IrisPreferences preferences;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		preferences = new IrisPreferences();
		
		proxyAddressTextField.setText(preferences.get(IrisPreferences.PROXY_ADDRESS,""));
		proxyPortTextField.setText(String.valueOf(preferences.getInt(IrisPreferences.PROXY_PORT, 0)));
	}
	
	/**
	 * OKボタン押下時のイベントハンドラ
	 * @param event
	 */
	public void okButtonPressed(ActionEvent event){
		savePreferences();
		stage.close();
	}
	
	/**
	 * 入力項目を保存する。
	 */
	private void savePreferences(){
		preferences.setValue(IrisPreferences.PROXY_ADDRESS, proxyAddressTextField.getText().trim());
		int port = 0;
		if (proxyPortTextField.getText().trim().length() != 0){
			try{
				port = Integer.parseInt(proxyPortTextField.getText().trim());
				preferences.setValue(IrisPreferences.PROXY_PORT, port);
			}catch(NumberFormatException e){
				log.debug("Proxyポート番号のパースに失敗 "+proxyAddressTextField.getText().trim());
			}
		}
	}
	
	public void cancelButtonPressed(ActionEvent event){
		stage.close();
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
}
