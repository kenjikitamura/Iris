package jp.rainbowdevil.iris;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import jp.rainbowdevil.bbslibrary.BbsConnector;
import jp.rainbowdevil.bbslibrary.BbsManager;
import jp.rainbowdevil.bbslibrary.model.Bbs;
import jp.rainbowdevil.bbslibrary.model.Board;
import jp.rainbowdevil.bbslibrary.model.Message;
import jp.rainbowdevil.bbslibrary.model.MessageThread;
import jp.rainbowdevil.bbslibrary.parser.BbsPerseException;
import jp.rainbowdevil.bbslibrary.parser.NichannelParser;
import jp.rainbowdevil.bbslibrary.repository.BbsRepository;
import jp.rainbowdevil.bbslibrary.utils.IOUtils;
import jp.rainbowdevil.iris.preferences.IrisPreferences;

public class BbsService {
	private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
			.getLogger(BbsService.class);
	private Bbs bbs;
	private List<Board> boardGroups;
	private BbsManager bbsManager;
	private BbsConnector bbsConnector;
	private IrisController controller;
	private BbsRepository bbsRepository;
	
	/** 現在表示中の板 */
	private Board currentBoard;
	
	/** 現在表示中のスレッド */
	private MessageThread currentMessageThread;
	
	public void init(){
		bbsManager = new BbsManager();
		bbs = new Bbs();
		bbs.setId("2ch_");
		bbs.setUrl("http://menu.2ch.net/bbsmenu.html");
		bbsConnector = bbsManager.createBbsConnector(bbs);
		bbsRepository = new BbsRepository();
		setProxy();
	}
	
	private void setProxy(){
		IrisPreferences preferences = new IrisPreferences();
		if (preferences.get(IrisPreferences.PROXY_ADDRESS, "").trim().length() != 0){
			String address = preferences.get(IrisPreferences.PROXY_ADDRESS);
			int port = preferences.getInt(IrisPreferences.PROXY_PORT, 8080);
			Proxy proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(address,port));
			bbsConnector.setProxy(proxy);
		}
	}
	
	/**
	 * 掲示板の板一覧をダウンロードする。
	 * 同時にレポジトリに保存する。
	 * 
	 * @return
	 * @throws IOException
	 * @throws BbsPerseException
	 */
	public List<Board> downloadBoardList() throws IOException, BbsPerseException{
		setProxy();
		InputStream inputStream = bbsConnector.getBoardGroup();
		byte[] bytes = IOUtils.toByteArray(inputStream);		
		NichannelParser parser = new NichannelParser();
		List<Board> boardGroups = parser.parseBbsMenu(new ByteArrayInputStream(bytes), bbs);
		
		// パースに成功したら保存
		bbsRepository.writeBoardList(bbs, bytes);
		return boardGroups;
	}
	
	public BbsConnector createBbsConnector(Bbs bbs){
		BbsConnector bbsConnector = new BbsConnector(bbs);
		return bbsConnector;
	}
	
	/**
	 * 板一覧を更新し、表示する。
	 */
	public void updateBoardList(boolean forceUpdate){
		List<Board> localBoardList = readBoardListFromRepository();
		if (localBoardList != null && !forceUpdate){
			log.debug("板一覧のキャッシュを使用");
			boardGroups = localBoardList;
			controller.showBoardList(localBoardList);
		}else{
			log.debug("サーバから板一覧を取得");
			try {
				List<Board> serverBoardList = downloadBoardList();
				boardGroups = serverBoardList;
				controller.showBoardList(serverBoardList);	
			} catch (IOException e) {
				controller.showErrorMessage("板一覧の取得に失敗。"+e.getClass().getName()+":"+e.getMessage());
			} catch (BbsPerseException e) {
				controller.showErrorMessage("板一覧のパースに失敗。"+e.getClass().getName()+":"+e.getMessage());
			}
		}
	}
	public void updateBoardList(){
		updateBoardList(false);
	}
	
	/**
	 * 最後に開いていたアイテムを表示する。
	 */
	public void openLastSelectedItem(){
		IrisPreferences preferences = new IrisPreferences();
		String boardId = preferences.get(IrisPreferences.LAST_SELECTED_BOARD_ID);
		log.debug("最後に開いていた板 ID="+boardId);
		if (boardId != null){
			for(Board board:boardGroups){
				if (board.hasChildren()){
					for(Board board2:board.getChildren()){
						if (board2.getId().equals(boardId)){
							log.debug("最後に開いていた板を開く id="+boardId+" board="+board2);
							controller.setSelection(board2);
							currentBoard = board2;
						}
					}
				}
			}
		}
		String messageThreadFilename = preferences.get(IrisPreferences.LAST_SELECTED_MESSAGE_THREAD_FILENAME);
		if (messageThreadFilename != null){
			MessageThread messageThread = new MessageThread();
			messageThread.setFilename(messageThreadFilename);
			controller.setSelection(messageThread);
			currentMessageThread = messageThread;
		}
	}
	
	
	
	/**
	 * 板一覧リストをキャッシュから読み込む
	 * @return
	 */
	public List<Board> readBoardListFromRepository(){
		try {
			InputStream inputStream = bbsRepository.loadBoardList(bbs);
			NichannelParser parser = new NichannelParser();
			boardGroups = parser.parseBbsMenu(inputStream, bbs);
			return boardGroups;
		} catch (IOException e){
			log.debug("保存した板一覧はなかった。");
		} catch (BbsPerseException e) {
			log.debug("保存した板一覧のパースに失敗",e);
		}
		return null;
	}
	
	public void openBoard(Board board){
		openBoard(board,false);
	}
	
	public void openBoard(Board board, boolean forceUpdate){
		log.debug("板を開きスレッド一覧を取得する url="+board.getUrl());
		List<MessageThread> localMessageThreads = readMessageThreadListFromRepository(board); 
		if (localMessageThreads != null && !forceUpdate){
			log.debug("スレッド一覧のキャッシュを使用");
			controller.showThreadList(localMessageThreads);
		}else{
			log.debug("サーバからスレッド一覧を取得");
			try {
				setProxy();
				InputStream inputStream = bbsConnector.getMessageThreadList(board);
				byte[] bytes = IOUtils.toByteArray(inputStream);
				NichannelParser parser = new NichannelParser();
				List<MessageThread> messageThreads = parser.parseMessageThreadList(new ByteArrayInputStream(bytes), board);
				controller.showThreadList(messageThreads);
				
				// 読み込みに成功したら保存
				log.debug(board.getTitle()+"をキャッシュに保存");
				bbsRepository.writeMessageThreadList(board, bytes);
				
				currentBoard = board;
				
			} catch (IOException e) {
				controller.showErrorMessage("スレッド一覧の取得に失敗。"+e.getClass().getName()+":"+e.getMessage());
			} catch (BbsPerseException e) {
				controller.showErrorMessage("スレッド一覧のパースに失敗。"+e.getClass().getName()+":"+e.getMessage());
			}
		}
	}
	
	/**
	 * スレッド一覧をキャッシュから読み込む
	 * 
	 * キャッシュに存在しない場合はnullを返す。
	 * 
	 * @param board
	 * @return
	 */
	public List<MessageThread> readMessageThreadListFromRepository(Board board){
		try {
			InputStream inputStream = bbsRepository.loadMessageThreadList(board);
			if (inputStream == null){
				return null;
			}else{
				NichannelParser parser = new NichannelParser();
				return parser.parseMessageThreadList(inputStream, board);
			}
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			log.debug("スレッド一覧のキャッシュ読み込み中に例外発生 ",e);
			return null;
		} catch (BbsPerseException e) {
			log.debug("スレッド一覧のキャッシュ読み込み中に例外発生 ",e);
			return null;
		}
	}
	
	public void openMessageThread(MessageThread messageThread){
		try{
			setProxy();
			InputStream inputStream = bbsConnector.getMessageList(messageThread, messageThread.getParentBoard());
			byte[] bytes = IOUtils.toByteArray(inputStream);
			
			NichannelParser parser = new NichannelParser();
			List<Message> messages = parser.parseMessageList(new ByteArrayInputStream(bytes), messageThread);
			for(Message message:messages){
				message.setParentMessageThread(messageThread);
			}
			controller.showMessageWebView(convertMessageListToHtml(messages));
			currentMessageThread = messageThread;

			bbsRepository.writeMessageThread(messageThread, bytes);
		} catch (IOException e){
			controller.showErrorMessage("メッセージ一覧の取得に失敗。"+e.getClass().getName()+":"+e.getMessage());
		} catch (BbsPerseException e) {
			controller.showErrorMessage("メッセージ一覧の取得に失敗。"+e.getClass().getName()+":"+e.getMessage());
		}
	}
	
	/**
	 * 現在表示中のスレッドを更新する。
	 */
	public void reloadMessageThread(){
		openMessageThread(currentMessageThread);
	}
	
	/**
	 * 現在表示中の板を更新する
	 */
	public void reloadBoard(){
		log.debug("表示中の板をリロード 板="+currentBoard);
		if (currentBoard != null){
			openBoard(currentBoard, true);
		}
	}
	
	private String convertMessageListToHtml(List<Message> messages){
		StringBuilder sb = new StringBuilder();
		int no = 1;
		sb.append("<div style=\"font-family:'ヒラギノ角ゴ Pro W3','Hiragino Kaku Gothic Pro','メイリオ',Meiryo,'ＭＳ Ｐゴシック',sans-serif;\">");
		for(Message message:messages){
			// No
			sb.append("<font color='blue'>"+no+"</font>");
			
			// Name
			sb.append(" : <font color='green'>"+message.getUserName()+"</b></font> ");
			
			// 投稿日
			sb.append(" : "+message.getSubmittedDateString());
			
			// ID
			sb.append(" ID:"+message.getUserHash()+" <br>");
			
			// 本文
			sb.append(message.getBody());
			
			sb.append("<br><br>");
			
			no++;
		}
		sb.append("</div>");
		
		// debug
		try{
			File file = new File("test.html");
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(sb.toString().getBytes());
			fileOutputStream.close();
		}catch(IOException e){
			log.error("error",e);
			
		}
		return sb.toString();
	}

	public List<Board> getBoardGroups() {
		return boardGroups;
	}

	public void setBoardGroups(List<Board> boardGroups) {
		this.boardGroups = boardGroups;
	}

	public IrisController getController() {
		return controller;
	}

	public void setController(IrisController controller) {
		this.controller = controller;
	}

	public Board getCurrentBoard() {
		return currentBoard;
	}

	public void setCurrentBoard(Board currentBoard) {
		this.currentBoard = currentBoard;
	}

	public MessageThread getCurrentMessageThread() {
		return currentMessageThread;
	}

	public void setCurrentMessageThread(MessageThread currentMessageThread) {
		this.currentMessageThread = currentMessageThread;
	}
}
