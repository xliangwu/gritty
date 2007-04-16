/**
 * 
 */
package com.wittams.gritty.jsch;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import com.wittams.gritty.Questioner;

class QuestionerUserInfo implements UserInfo, UIKeyboardInteractive{
	private Questioner questioner;
	private String passwd;
	private String passPhrase;
	
	public QuestionerUserInfo(Questioner questioner){
		this.questioner = questioner;
	}
	
	public String getPassphrase(){
		return passPhrase;
	}

	public String getPassword(){
		// TODO Auto-generated method stub
		return passwd;
	}

	public boolean promptPassphrase(String message){
		passPhrase = questioner.questionHidden(message + ":");
		return true;
	}

	public boolean promptPassword(String message){
		passwd = questioner.questionHidden(message + ":");
		return true;
	}

	public boolean promptYesNo(String message){
		String yn = questioner.questionVisible(message + " [Y/N]:" , "Y");
		String lyn = yn.toLowerCase();
		if( lyn.equals("y") || lyn.equals("yes") ){
			return true;
		}else{
			return false;
		}
	}

	public void showMessage(String message){
		questioner.showMessage(message);
	}

	public String[] promptKeyboardInteractive(final String destination, final String name,
			final String instruction, final String[] prompt, final boolean[] echo){
		return null ;
	}
}