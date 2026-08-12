/**
 * JSBridge - H5 与原生的通信桥梁
 * 游戏通关/失败/关闭时调用原生方法
 */
var NativeBridge = {
    /** 通知原生：放生成功 */
    onGameWin: function(gameType, score) {
        if (window.AndroidBridge) {
            window.AndroidBridge.onGameWin(gameType, score);
        }
        console.log('放生成功:', gameType, '分数:', score);
    },
    
    /** 通知原生：游戏失败 */
    onGameLose: function(gameType) {
        if (window.AndroidBridge) {
            window.AndroidBridge.onGameLose(gameType);
        }
        console.log('游戏失败:', gameType);
    },
    
    /** 通知原生：关闭游戏页面 */
    closeGame: function() {
        if (window.AndroidBridge) {
            window.AndroidBridge.closeGame();
        }
        console.log('关闭游戏');
    },
    
    /** 通知原生：分数变化 */
    onScoreChanged: function(score) {
        if (window.AndroidBridge) {
            window.AndroidBridge.onScoreChanged(score);
        }
    }
};
