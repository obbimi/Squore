String.prototype.padLeft = function padLeft(length, leadingChar) {
    if (leadingChar === undefined) leadingChar = "0";
    return this.length < length ? (leadingChar + this).padLeft(length, leadingChar) : this;
};
String.prototype.format = function () {
    var args = arguments;
    if ( (args.length === 1) && (args[0].constructor === Array) ) {
        args = args[0];
    }
    return this.replace(/\{(\d+)\}/g, function (m, n) { return args[n] || ''; });
};

var CountDownTimer = {
    m_iSecondsLeft : 0,
    m_iInterval    : null,
    show : function(iStartAt) {
        clearInterval(CountDownTimer.m_iInterval);
        this.m_iSecondsLeft = iStartAt;
        this.setTime();
        $('#btn_timer').fadeIn(1000);
        this.m_iInterval    = setInterval(function() {
            CountDownTimer.m_iSecondsLeft--;
            CountDownTimer.setTime();
            // If the count down is finished, write some text
            if (CountDownTimer.m_iSecondsLeft <= 0) {
                clearInterval(CountDownTimer.m_iInterval);
                $('#btn_timer').html("Time!"); // TODO: from message from app
            }
        }, 1000);
    },
    setTime : function() {
        var sSec = ("" + CountDownTimer.m_iSecondsLeft % 60).padLeft(2, '0');
        var sMin = "" + (CountDownTimer.m_iSecondsLeft - sSec)/60;
        $('#btn_timer').html(sMin + ':' + sSec);
    },
    cancel : function() {
        clearInterval(CountDownTimer.m_iInterval);
        $('#btn_timer').fadeOut(1000);
    }
};
var Call = {
    showDecision : function(sMsg, iIndxP, sCall, bIsConduct) {
        var oMsg = $('#callDecisionMessage' + (iIndxP+1));
        oMsg.find('span').html(sMsg);
        oMsg.fadeIn(1000);
        setTimeout(function() {
            Call.hideDecision(iIndxP);
        }, 5000);
    },
    hideDecision : function(iIndxP) {
        var oMsg = $('#callDecisionMessage' + (iIndxP+1));
        oMsg.fadeOut(1000);
    }
};
var GameScores = {
    update : function(lomPlayer2Score, bSwapAAndB) {
        console.log(lomPlayer2Score);
        $('#gameScores > table > tbody').empty();
        lomPlayer2Score.forEach(function (item, index) {
            console.log(index);
            console.log(item);
            var sA = '<td class="digit is_winner_{1}">{0}</td>'.format(item.A, item.A > item.B);
            var sB = '<td class="digit is_winner_{1}">{0}</td>'.format(item.B, item.B > item.A);
            var sTR = '<tr>{0}<td class="splitter">-</td>{1}</tr>'.format(sA, sB);
            if ( bSwapAAndB ) {
                var sTR = '<tr>{0}<td class="splitter">-</td>{1}</tr>'.format(sB, sA);
            }
            $('#gameScores > table > tbody').append(sTR);
        });
    },
    display : function(bShow) {
        if ( bShow ) {
            $('#gameScores').show();
            $('.gameswonbutton').hide();
        } else {
            $('#gameScores').hide();
            $('.gameswonbutton').show();
        }
    }
};
var iTxtPlayerCount = 0;
function handleMessage(customEvent)
{
console.log(customEvent);
    var data = customEvent.data;
    if ( data.id ) {
console.log(data.id + '.'  + (data.property || 'text') + ' = ' + data.value);
        if ( (! data.property) || data.property === 'text' ) {
            $('#' + data.id + ' > span').html(data.value);
            if ( data.id.indexOf('txt_player')===0 ) {
                var iTextLength      = data.value.length;
                var sIdOther         = data.id==='txt_player1'?'txt_player2':'txt_player1';
                var iTextLengthOther = $('#' + sIdOther + ' > span').html().length;
                var sIdToResize      = iTextLength > iTextLengthOther ? data.id : sIdOther;
              //console.log('Resize to fit ' + sIdToResize);
                //$('#' + data.id + ' > span').fitText(0.75); // does not work if there is a slash in the text
                $('#' + sIdToResize).textfill({ success: function(o, fontSizeFinal) {
                      //console.log('Resized to fit - success : ' + fontSizeFinal);
                        fontSizeFinal = fontSizeFinal * 0.9; // to have a little spacing
                        $('#' + data.id  + ' > span').css('font-size', fontSizeFinal);
                        $('#' + sIdOther + ' > span').css('font-size', fontSizeFinal);
                    }
                });
                iTxtPlayerCount++;
                if ( iTxtPlayerCount === 2 ) {
                    // names of both players received, time two remove the 'loading...' message
                    $('#squoreboard_loading').fadeOut(1000);
                    $('#squoreboard_root_view').fadeIn(1000);
                }
            }
        } else if ( data.property === 'src' ) {
            $('#' + data.id).attr(data.property, data.value);
        } else if ( data.property === 'display' ) {
            if ( data.value === 'none' ) {
                $('#' + data.id ).fadeOut(1000);
            } else {
                $('#' + data.id ).fadeIn(1000);
            }
        } else {
            $('#' + data.id ).css(data.property, data.value);
        }
    }
    if ( data.func ) {
console.log(data.func);
        eval(data.func);
    }
}

var CHANNEL_SB = 'urn:x-cast:com.doubleyellow.scoreboard';
var CHANNEL_BM = 'urn:x-cast:com.doubleyellow.badminton';
var CHANNEL_TP = 'urn:x-cast:com.doubleyellow.tennispadel';
var CHANNEL_TT = 'urn:x-cast:com.doubleyellow.tabletennis';

function window_onload() {
    $('#squoreboard_root_view').hide();
    $('#squoreboard_root_view').find('span').html('');
    CountDownTimer.cancel();
    Call.hideDecision(0);
    Call.hideDecision(1);
    GameScores.display(false);
    $('#gameBallMessage').hide();

    var ctx     = cast.framework.CastReceiverContext.getInstance();
//console.log('ctx:' + ctx);
    var options = new cast.framework.CastReceiverOptions();           // https://developers.google.com/cast/docs/reference/caf_receiver/cast.framework.CastReceiverOptions
    options.disableIdleTimeout = true;                                // With this set to true, the receiver no longer times out after 5 minutes

    // This is used to send data back to the senders apps.
    options.customNamespaces = Object.assign({});
    options.customNamespaces[CHANNEL_BM] = cast.framework.system.MessageType.JSON;
//console.log('options:' + options);
//console.log(options);

    ctx.addCustomMessageListener(CHANNEL_SB, handleMessage);
    ctx.addCustomMessageListener(CHANNEL_BM, handleMessage);
    ctx.addCustomMessageListener(CHANNEL_TP, handleMessage);
    ctx.addCustomMessageListener(CHANNEL_TT, handleMessage);

    ctx.start(options);
}
