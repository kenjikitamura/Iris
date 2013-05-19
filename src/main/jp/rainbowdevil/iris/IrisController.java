package jp.rainbowdevil.iris;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import jp.rainbowdevil.bbslibrary.model.Board;
import jp.rainbowdevil.bbslibrary.model.MessageThread;

public class IrisController implements Initializable{
	private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(IrisController.class);
	private BbsService service;
	private TreeItem<Board> rootNode = new TreeItem<Board>(new Board());
	
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
	private Label messageThreadTitleLabel;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		log.debug("Initialize");
		service = new BbsService();
		service.setController(this);
		service.init();
		
		service.updateBoardList();
		//boardTreeView.getSelectionModel().
		boardTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		boardTreeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
			@Override
			public void changed(ObservableValue observable, Object oldValue,
					Object newValue) {
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
	
	private void setupTreeItem(TreeItem<Board> parentTreeItem, Board board){
		TreeItem<Board> treeItem = new TreeItem<Board>(board);
		parentTreeItem.getChildren().add(treeItem);
		if (board.hasChildren()){
			for(Board childBoard:board.getChildren()){
				setupTreeItem(treeItem, childBoard);
			}
		}
	}
}
