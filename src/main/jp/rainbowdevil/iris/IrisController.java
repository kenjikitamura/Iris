package jp.rainbowdevil.iris;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import jp.rainbowdevil.bbslibrary.model.Board;
import jp.rainbowdevil.bbslibrary.model.MessageThread;
import jp.rainbowdevil.iris.preferences.IrisPreferences;
import jp.rainbowdevil.iris.preferences.PreferencesController;

public class IrisController implements Initializable{
	private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(IrisController.class);
	private BbsService service;
	private TreeItem<Board> rootNode = new TreeItem<Board>(new Board());
	
	/** 最大化用 */
    private Rectangle2D backupWindowBounds = null;
    private boolean maximized = false;
    
    private Stage stage;
	private WebEngine webEngine;
	
	@FXML private AnchorPane topPane;
	@FXML private Button boardListReloadButton;
	@FXML private TreeView<Board> boardTreeView;
	@FXML private ListView<MessageThread> messageThreadListView;
	@FXML private WebView messageView;
	@FXML private ToolBar boardListToolbar;
	@FXML private ToolBar threadListToolbar;
	@FXML private ToolBar messageListToolbar;
	@FXML private Label messageThreadTitleLabel;
	@FXML private MenuBar menuBar;
	@FXML private SplitPane leftSplitPane;
	@FXML private SplitPane rightSplitPane;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		log.debug("Controller.initialize");
		service = new BbsService();
		service.setController(this);
		service.init();
		
		service.updateBoardList();
		boardTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		boardTreeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
			@Override
			public void changed(ObservableValue observable, Object oldValue,
					Object newValue) {
				if (boardTreeView.getSelectionModel().getSelectedItem() == null){
					return;
				}
				Board board = (Board)boardTreeView.getSelectionModel().getSelectedItem().getValue();
				if (board.getUrl() == null){
					return;
				}
				log.debug("選択 "+board);
				service.openBoard(board);
			}
		});
		
		messageThreadListView.setCellFactory(new Callback<ListView<MessageThread>, ListCell<MessageThread>>() {
            @Override
            public ListCell<MessageThread> call(ListView<MessageThread> param) {
                return new MessageThreadCell();
            }
        });
		messageThreadListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
			@Override
			public void changed(ObservableValue observable, Object oldValue,
					Object newValue) {
				MessageThread messageThread = (MessageThread)messageThreadListView.getSelectionModel().getSelectedItem();
				if (messageThread != null){
					messageThreadTitleLabel.setText(messageThread.getTitle());
					service.openMessageThread(messageThread);
				}
			}
		});
		webEngine = messageView.getEngine();
		setupToolbar(boardListToolbar);
		setupToolbar(messageListToolbar);
		setupToolbar(threadListToolbar);
		
		if (IrisWindow.isMac()){
			menuBar.setUseSystemMenuBar(true);
		}else{
			menuBar.setUseSystemMenuBar(false);
		}
	}
	
	private double mouseDragOffsetX = 0;
	private double mouseDragOffsetY = 0;
	
	private void setupToolbar(ToolBar toolBar){
        toolBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                mouseDragOffsetX = event.getSceneX();
                mouseDragOffsetY = event.getSceneY();
            }
        });
        toolBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
            	Stage stage = (Stage) topPane.getScene().getWindow();
                stage.setX(event.getScreenX()-mouseDragOffsetX);
                stage.setY(event.getScreenY()-mouseDragOffsetY);
            }
        });
	}
	
	public void showMessageWebView(String content){
		webEngine.loadContent(content);
	}
	
	/**
	 * 板一覧を表示
	 * 
	 */
	public void showBoardList(List<Board> boards){
		boardTreeView.setRoot(rootNode);
		rootNode.getValue().setTitle("2ch");
		rootNode.setExpanded(true);
		for(Board board:boards){
			setupTreeItem(rootNode, board);
		}
	}
	
	public void showErrorMessage(String message){
		log.debug("エラーメッセージを表示する。 "+message);
	}
	
	/**
	 * スレ一覧を表示
	 * @param board
	 */
	public void showThreadList(List<MessageThread> messageThreads){
		messageThreadListView.getItems().clear();
		messageThreadListView.getItems().addAll(messageThreads);
	}
	
	/**
	 * 終了ボタン押下
	 * @param e
	 */
	public void exit(ActionEvent e) {
		//Platform.exit();
		stage.close();
	}
	
	/**
	 * 最小化ボタン押下
	 * @param e
	 */
	public void minimized(ActionEvent e){		
		stage.setIconified(true);
	}
	
	/**
	 * 最大化ボタン押下
	 * @param e
	 */
	public void maxmized(ActionEvent e){
		toogleMaximized();
	}
	
	/**
	 * 最大化、最大化解除を行う
	 */
    public void toogleMaximized() {        
        final Screen screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1).get(0);
        if (maximized) {
            maximized = false;
            if (backupWindowBounds != null) {
                stage.setX(backupWindowBounds.getMinX());
                stage.setY(backupWindowBounds.getMinY());
                stage.setWidth(backupWindowBounds.getWidth());
                stage.setHeight(backupWindowBounds.getHeight());
            }
        } else {
            maximized = true;
            backupWindowBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            stage.setX(screen.getVisualBounds().getMinX());
            stage.setY(screen.getVisualBounds().getMinY());
            stage.setWidth(screen.getVisualBounds().getWidth());
            stage.setHeight(screen.getVisualBounds().getHeight());
        }
    }
	
	private void setupTreeItem(TreeItem<Board> parentTreeItem, Board board){
		TreeItem<Board> treeItem = new TreeItem<Board>(board);
		parentTreeItem.getChildren().add(treeItem);
		if (board.hasChildren()){
			for(Board childBoard:board.getChildren()){
				setupTreeItem(treeItem, childBoard);
			}
		}
	}
	
	//-------------------------------------------------------
	// メニュー
	
	/**
	 * 設定画面を開く
	 * @param event
	 */
	public void openPreferences(ActionEvent event){
		Stage stage = new Stage();
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/PreferencesWindow.fxml"));
			final AnchorPane root = (AnchorPane) loader.load();
		    PreferencesController controller = (PreferencesController)loader.getController();
		        controller.setStage(stage);
			stage.setScene(new Scene(root));
		    stage.setTitle("My modal window");
		    stage.initModality(Modality.APPLICATION_MODAL);
		    stage.show();
		} catch (IOException e) {
			log.error("設定画面のオープンに失敗",e);
		}
	}
	
	/**
	 * ウインドウの状態を保存する。
	 */
	public void saveWindowState(){
		log.debug("ウインドウ高さ="+stage.getHeight()+" 幅="+stage.getWidth()+" 板幅="+leftSplitPane.getDividerPositions()[0]+" メッセージ幅="+rightSplitPane.getDividerPositions()[0]);
		IrisPreferences preferences = new IrisPreferences();
		preferences.setValue(IrisPreferences.WINDOW_HEIGHT, stage.getHeight());
		preferences.setValue(IrisPreferences.WINDOW_WIDTH, stage.getWidth());
		preferences.setValue(IrisPreferences.WINDOW_DIVIDER_POSITION1, leftSplitPane.getDividerPositions()[0]);
		preferences.setValue(IrisPreferences.WINDOW_DIVIDER_POSITION2, rightSplitPane.getDividerPositions()[0]);
		try {
			preferences.save();
		} catch (BackingStoreException e) {
			log.error("設定の保存に失敗",e);
		}
	}
	
	/**
	 * 設定値からウインドウの状態を復元する。
	 */
	public void loadWindowState(){
		IrisPreferences preferences = new IrisPreferences();
		stage.setHeight(preferences.getDouble(IrisPreferences.WINDOW_HEIGHT, 600f));
		stage.setWidth(preferences.getDouble(IrisPreferences.WINDOW_WIDTH, 900f));
		double[] div1 = new double[1];
		div1[0] = preferences.getDouble(IrisPreferences.WINDOW_DIVIDER_POSITION1, 0.3f); 
		leftSplitPane.setDividerPositions(div1);
		double[] div2 = new double[1];
		div2[0] = preferences.getDouble(IrisPreferences.WINDOW_DIVIDER_POSITION2, 0.5f); 
		rightSplitPane.setDividerPositions(div2);
		log.debug("ウインドウサイズ復元 width="+preferences.getDouble(IrisPreferences.WINDOW_WIDTH, 600f)+
				" height="+preferences.getDouble(IrisPreferences.WINDOW_HEIGHT, 900f)+
				" split1="+preferences.getDouble(IrisPreferences.WINDOW_DIVIDER_POSITION1, 0.3f)+
				" split2="+preferences.getDouble(IrisPreferences.WINDOW_DIVIDER_POSITION2, 0.5f));
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		log.debug("setStage "+stage);
		this.stage = stage;
	}
}
