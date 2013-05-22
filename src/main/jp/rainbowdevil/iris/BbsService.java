package jp.rainbowdevil.iris;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

public class BbsService {
	private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
			.getLogger(BbsService.class);
	private Bbs bbs;
	private List<Board> boardGroups;
	private BbsManager bbsManager;
	private BbsConnector bbsConnector;
	private IrisController controller;
	private BbsRepository bbsRepository;
	
	public void init(){
		bbsManager = new BbsManager();
		bbs = new Bbs();
		bbs.setId("2ch_");
		bbs.setUrl("http://menu.2ch.net/bbsmenu.html");
		bbsConnector = bbsManager.createBbsConnector(bbs);
		bbsRepository = new BbsRepository();
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
		InputStream inputStream = bbsConnector.getBoardGroup();
		byte[] bytes = IOUtils.toByteArray(inputStream);		
		NichannelParser parser = new NichannelParser();
		List<Board> boardGroups = parser.parseBbsMenu(new ByteArrayInputStream(bytes));
		
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
	public void updateBoardList(){
		List<Board> localBoardList = readBoardListFromRepository();
		if (localBoardList != null){
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
	
	public List<Board> readBoardListFromRepository(){
		try {
			InputStream inputStream = bbsRepository.loadBoardList(bbs);
			NichannelParser parser = new NichannelParser();
			boardGroups = parser.parseBbsMenu(inputStream);
			return boardGroups;
		} catch (IOException e){
			log.debug("保存した板一覧はなかった。");
		} catch (BbsPerseException e) {
			log.debug("保存した板一覧のパースに失敗",e);
		}
		return null;
	}
	
	public void openBoard(Board board){
		try {
			List<MessageThread> messageThreads = bbsConnector.getMessageThreadList(board);
			controller.showThreadList(messageThreads);
		} catch (IOException e) {
			controller.showErrorMessage("スレッド一覧の取得に失敗。"+e.getClass().getName()+":"+e.getMessage());
		} catch (BbsPerseException e) {
			controller.showErrorMessage("スレッド一覧のパースに失敗。"+e.getClass().getName()+":"+e.getMessage());
		}
	}
	
	public void openMessageThread(MessageThread messageThread){
		try{
			List<Message> messages = bbsConnector.getMessageList(messageThread, messageThread.getParentBoard());
			controller.showMessageWebView(convertMessageListToHtml(messages));
		} catch (IOException e){
			controller.showErrorMessage("メッセージ一覧の取得に失敗。"+e.getClass().getName()+":"+e.getMessage());
		} catch (BbsPerseException e) {
			controller.showErrorMessage("メッセージ一覧の取得に失敗。"+e.getClass().getName()+":"+e.getMessage());
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
			sb.append(" : <font color='green'>"+message.getUserName()+"</font> ");
			
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
}
