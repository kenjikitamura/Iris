package jp.rainbowdevil.iris;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import jp.rainbowdevil.bbslibrary.model.Board;
import jp.rainbowdevil.bbslibrary.model.MessageThread;

public class IrisController implements Initializable{
	private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(IrisController.class);
	private BbsService service;
	private TreeItem<Board> rootNode = new TreeItem<Board>(new Board());
	
	/** 最大化用 */
    private Rectangle2D backupWindowBounds = null;
    private boolean maximized = false;
    
    private Stage stage;
	
	@FXML
	private AnchorPane topPane;
	
	@FXML
	private Button boardListReloadButton;
	
	@FXML
	private TreeView<Board> boardTreeView;
	
	@FXML
	private ListView<MessageThread> messageThreadListView;
	
	private WebEngine webEngine;
	@FXML
	private WebView messageView;
	
	@FXML
	private ToolBar boardListToolbar;
	
	@FXML
	private ToolBar threadListToolbar;
	
	@FXML
	private ToolBar messageListToolbar;
	
	@FXML
	private Label messageThreadTitleLabel;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		log.debug("Initialize");
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
		Platform.exit();
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

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
}
