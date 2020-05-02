// https://gist.github.com/jarettmillard/398698c191a20e0a1ebc44b593f5729f

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
var Changer = {
    setText : function(id, txt) {
        try {
            $('#' + id + ' > span').text(txt);
            //$('#' + id + ' > span').innerText = txt;
        } catch {
            m_castLogger.info(LOG_TAG,'Could not set span text for {0}={1}'.format(id, txt));
        }
    }
};

var CountDownTimer = {
    m_iSecondsLeft : 0,
    m_iInterval    : null,
    show : function(iStartAt) {
        clearInterval(CountDownTimer.m_iInterval);
        this.m_iSecondsLeft = iStartAt;
        this.setTime();
        resizeToFit('btn_timer');
        $('#btn_timer').fadeIn(1000);
        this.m_iInterval    = setInterval(function() {
            CountDownTimer.m_iSecondsLeft--;
            CountDownTimer.setTime();
            // If the count down is finished, write some text
            if ( CountDownTimer.m_iSecondsLeft <= 0 ) {
                clearInterval(CountDownTimer.m_iInterval);
                Changer.setText('btn_timer', "Time!"); // TODO: from message from app
                resizeToFit('btn_timer');
            }
        }, 1000);
    },
    setTime : function() {
        var sSec = ("" + CountDownTimer.m_iSecondsLeft % 60).padLeft(2, '0');
        var sMin = "" + (CountDownTimer.m_iSecondsLeft - sSec)/60;
        Changer.setText('btn_timer', sMin + ':' + sSec);
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
    update : function(lomPlayer2Score, bSwapAAndB, loiGameDuration, bMatchIsFinished) {
        m_castLogger.debug(LOG_TAG, JSON.stringify(lomPlayer2Score));
        $('#gameScores > table > tbody').empty();
        lomPlayer2Score.forEach(function (item, index) {
            m_castLogger.debug(LOG_TAG, 'index:' + index);
            m_castLogger.debug(LOG_TAG, 'item ' + JSON.stringify(item));
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
var GameGraph = {
    // http://dygraphs.com/
    show: function(iGameNr1B, iNrOfPointsToWin, sScoreSequence, bGameIsFinished) {
        this.parseGraphData(iNrOfPointsToWin, sScoreSequence);

        var oDiv = document.getElementById('gameGraph');
        if ( ! oDiv ) {
            m_castLogger.warn('No div for graph');
            return;
        }
        var options = {
                    labels : [ "Points"
                             , $('#txt_player1').text().trim()
                             , $('#txt_player2').text().trim()
                             ],
                    interactionModel: {},
                    valueRange: [ 0, parseInt(this.iMax) + 1 ],
                    axes : { y: { pixelsPerLabel: 12 }
                           , x: { pixelsPerLabel: 20
                                , independentTicks: true
                                }
                           },
                    gridLineColor: 'rgb(0,0,0)',
                    legend: 'always',
                    //title : 'Game' + ' ' + iGameNr1B
                 };
        var g = new Dygraph( oDiv, this.aGameCsv, options );
        $('.dygraph-legend').css('background-color', 'transparent'); // impossible with css because overwritten with jscode
    },
    aGameCsv : [],
    iMax     : 0,
    parseGraphData : function(iNrOfPointsToWin, sScoreSequence) {
        let aTmp = [];
        let iSeq = 0;
        let iA   = 0;
        let iB   = 0;
        aTmp.push([iSeq, iA, iB]);
        for(let i=0; i < sScoreSequence.length; i++) {
            if        ( sScoreSequence.substr(i,1)==='A' || sScoreSequence.substr(i,1)==='0' ) {
                iA++;
            } else if ( sScoreSequence.substr(i,1)==='B' || sScoreSequence.substr(i,1)==='1' ) {
                iB++;
            } else {
                continue;
            }
            aTmp.push([++iSeq, iA, iB]);
        }
        for ( let i=Math.max(iA, iB); i <= iNrOfPointsToWin; i++ ) {
            aTmp.push([++iSeq]);
        }
        this.iMax = Math.max(iA, iB, iNrOfPointsToWin);
        this.aGameCsv = aTmp;
    }
};
var iTxtPlayerCount = 0;
function resizeToFit(id) {
    var eToResize = $('#' + id);
    if ( eToResize.hasClass('noshrink') == false ) {
        eToResize.textfill({
            debug  : false,
          //success: function(o, fontSizeFinal) { m_castLogger.info(LOG_TAG,'Resized to fit - success : {0} ({1} = {2})'.format(fontSizeFinal, o.id, o.innerText)); },
            fail   : function(o)                { m_castLogger.warn(LOG_TAG,'Resized to fit - failed: {0} ...'.format(o.id)); }
        });
    }
}
var senderIds = {};
function handleMessageLS(customEvent) {
    handleMessage(customEvent, LOG_TAG + '.LS')
}
function handleMessage(customEvent, logTag)
{
    if ( cast.isDummy ) {
        // for non-dummy events, the caf framework logs messages in the console already
        m_castLogger.debug(logTag, 'customEvent:' + JSON.stringify(customEvent.data)); // typically { "type":"message", "defaultPrevented":false, "senderId":"<GUID>:com.doubleyellow.scoreboard-164", "data": {}
    }
    if ( ! senderIds[customEvent.senderId] ) {
        m_castLogger.warn(logTag,'senderId:' + customEvent.senderId);
        senderIds[customEvent.senderId] = 1;
    }
    var data = customEvent.data;

    if ( Array.isArray(data) ) {
        // several changes at once
        var aChanges = data;
        for(var i=0; i < aChanges.length; i++) {
          //m_castLogger.info(logTag, "Item " + i);
            handleData(aChanges[i], logTag);
        }
    } else {
        handleData(data, logTag);
    }
    if ( false ) {
        //m_castLogger.info(logTag, 'sending message back 1');
        //m_crContext.sendCustomMessage(CHANNEL_SB,                       JSON.stringify(data)); // does not work
        //m_crContext.sendCustomMessage(CHANNEL_SB, undefined           , JSON.stringify(data)); // results in ignored messages in adb logcat
        //m_crContext.sendCustomMessage(CHANNEL_SB, customEvent.senderId, JSON.stringify(data)); // results in ignored messages in adb logcat
        //m_castLogger.info(logTag, 'sending message back 2');
        //m_crContext.sendCustomMessage(CHANNEL_SB, customEvent.senderId, { type: 'status' , message: 'Playing' });
        //m_castLogger.info(logTag, 'sending message back 3');

        //m_crMessageBus.send(customEvent.senderId, data);
        m_crMessageBus.send(customEvent.senderId, 'OK');

        //m_crMessageBus.broadcast('OK'); // send a message to ALL connected devices

        //var castChannel = m_crMessageBus.getCastChannel(customEvent.senderId);
        //m_castLogger.info(logTag, 'castChannel : ' + castChannel);
    }
}
function handleData(data, logTag) {
    if ( data.id ) {
m_castLogger.debug(logTag, data.id + '.'  + (data.property || 'text') + ' = ' + data.value);
        if ( (! data.property) || data.property === 'text' ) {
            // change the text
            Changer.setText(data.id, data.value);

            // take some special action based on target that was updated
            if ( data.id.indexOf('txt_player')===0 ) {
                var iTextLength      = data.value.length;
                var sIdOther         = data.id==='txt_player1'?'txt_player2':'txt_player1';
                var eOther           = $('#' + sIdOther + ' > span');
                var iTextLengthOther = 0;
                var sIdToResize      = data.id;
                if ( eOther && eOther.length ) {
                    var sHtmlOther       = eOther.html();
                        iTextLengthOther = sHtmlOther.length;
                    if ( eOther.textContent ) {
                        // svg
                        iTextLengthOther = eOther.textContent.length;
                    }
                    var sIdToResize      = iTextLength > iTextLengthOther ? data.id : sIdOther;
                }
//m_castLogger.info(logTag,'Resize to fit ' + sIdToResize);
                var eToResize = $('#' + sIdToResize);
                if ( eToResize.textfill ) {
                    eToResize.textfill({
                        debug    : false,
                        widthOnly: true,
                        success: function(o, fontSizeFinal) {
//m_castLogger.info(logTag,'Resized to fit - success : ' + fontSizeFinal);
                            Changer.fontSizeGlobal = fontSizeFinal * 0.9; // to have a little spacing
                            $('#' + data.id  + ' > span').css('font-size', Changer.fontSizeGlobal);
                            $('#' + sIdOther + ' > span').css('font-size', Changer.fontSizeGlobal);
                        },
                        fail : function(o) {
m_castLogger.info(logTag,'Resized to fit - failed ...' + o.id);
                        }
                    });
                }
                iTxtPlayerCount++;
                if ( iTxtPlayerCount === 2 ) {
                    // names of both players received, time to remove the 'loading...' message
                    $('#squoreboard_loading').fadeOut(1000);
                    $('#squoreboard_root_view').fadeIn(1000);
                    m_crManager.setApplicationState("Displaying score"); // displayed as status when selecting device on handheld
                }
            } else {
                resizeToFit(data.id);
                // $('#' + data.id + ' > span').css('font-size', Changer.fontSizeGlobal);
            }
        } else {
            var eTarget = $('#' + data.id);
            if ( eTarget.hasClass('no-' + data.property) ) {
                m_castLogger.info(logTag,'Deliberatly not setting property {0} for {1}'.format(data.property, data.id));
            } else {
                if ( data.property === 'src' ) {
                    // flags
                    eTarget.attr(data.property, data.value);
                    if ( data.value === '' ) {
                        eTarget.fadeOut(1000);
                    } else {
                        eTarget.fadeIn(1000);
                    }
                } else if ( data.property === 'display' ) {
                    if ( data.value === 'none' ) {
                        eTarget.fadeOut(1000);
                    } else {
                        eTarget.fadeIn(1000);
                    }
                } else {
                    eTarget.css(data.property, data.value);
                    if ( data.property === 'background-image') {
                        if ( data.value === 'url()' ) {
                            eTarget.fadeOut(1000);
                        } else {
                            eTarget.fadeIn(1000);
                        }
                    }
                }
            }
        }
    }
    if ( data.func ) {
m_castLogger.info(logTag, data.func);
        eval(data.func);
    }
}

const CHANNEL_SB = 'urn:x-cast:com.doubleyellow.scoreboard';
const CHANNEL_BM = 'urn:x-cast:com.doubleyellow.badminton';
const CHANNEL_TP = 'urn:x-cast:com.doubleyellow.tennispadel';
const CHANNEL_TT = 'urn:x-cast:com.doubleyellow.tabletennis';

var m_crContext    = null;
var m_crManager    = null;
var m_crMessageBus = null;

function window_onload() {
    // Debug Logger

    if ( m_bDebug ) {
        m_castLogger = cast.debug.CastDebugLogger.getInstance();

        // Enable debug logger and show a 'DEBUG MODE' overlay at top left corner.
        m_castLogger.setEnabled(true);

        // Set verbosity level for Core events.
        m_castLogger.loggerLevelByEvents = {
          'cast.framework.events.category.CORE'         : cast.framework.LoggerLevel.DEBUG,
          'cast.framework.events.EventType.MEDIA_STATUS': cast.framework.LoggerLevel.DEBUG
        }

        // Set verbosity level for custom tags.
        m_castLogger.loggerLevelByTags = {
            LOG_TAG: cast.framework.LoggerLevel.DEBUG,
        };

        // Show debug overlay
        m_castLogger.showDebugLogs(true);
    } else {
        m_castLogger = DebugLogger.getInstance();
    }

    $('#squoreboard_root_view').hide();
    $('#squoreboard_root_view').find('span').html('');
    CountDownTimer.cancel();
    Call.hideDecision(0);
    Call.hideDecision(1);
    GameScores.display(false);
    $('#gameBallMessage').hide();

    m_crContext     = cast.framework.CastReceiverContext.getInstance();
//m_castLogger.info(LOG_TAG,'m_crContext:' + m_crContext);
    var crOptions = new cast.framework.CastReceiverOptions();           // https://developers.google.com/cast/docs/reference/caf_receiver/cast.framework.CastReceiverOptions
    crOptions.disableIdleTimeout = true;                                // With this set to true, the receiver no longer times out after 5 minutes

    // This is used to send data back to the senders apps.
    crOptions.customNamespaces = Object.assign({});
    crOptions.customNamespaces[CHANNEL_SB] = cast.framework.system.MessageType.STRING; // must be set to string for message bus ?! (Invalid messageType for the namespace)
    crOptions.customNamespaces[CHANNEL_SB] = cast.framework.system.MessageType.JSON;
/*
    crOptions.customNamespaces = {
        [CHANNEL_SB]: cast.framework.system.MessageType.JSON
    };
*/
//m_castLogger.info(LOG_TAG,'crOptions:' + crOptions);
//m_castLogger.info(LOG_TAG,crOptions);

    // this does NOT seem to work any more if a castmessage bus is created for the namespace as well
    // on the other hand, if NOT added the messagebus does not seem to work either ?!
    m_crContext.addCustomMessageListener(CHANNEL_SB, handleMessageLS);
    m_crContext.addCustomMessageListener(CHANNEL_BM, handleMessageLS);
    m_crContext.addCustomMessageListener(CHANNEL_TP, handleMessageLS);
    m_crContext.addCustomMessageListener(CHANNEL_TT, handleMessageLS);

    if ( true ) {
        m_crManager = cast.receiver.CastReceiverManager.getInstance(); // received by cast.framework.CastReceiverContext
        m_crManager.onSenderDisconnected = function(event) {
            m_castLogger.info(LOG_TAG, 'event.reason: ' + event.reason + '(cast.receiver.system.DisconnectReason.REQUESTED_BY_SENDER='+ cast.receiver.system.DisconnectReason.REQUESTED_BY_SENDER + ')');
            m_castLogger.info(LOG_TAG, 'm_crManager.getSenders().length: ' + this.getSenders().length);
              if (  ( m_crManager.getSenders().length == 0 )
                 && ( event.reason == cast.receiver.system.DisconnectReason.REQUESTED_BY_SENDER )
                 ) {
                  window.close();
              }
        }

        // must be requested before 'start()'
        m_crMessageBus = m_crManager.getCastMessageBus(CHANNEL_SB, cast.receiver.CastMessageBus.MessageType.JSON  ); // if using this, event.data is an object
      //m_crMessageBus = m_crManager.getCastMessageBus(CHANNEL_SB, cast.receiver.CastMessageBus.MessageType.STRING); // if using this, event.data is a string containing JSON
        m_crManager.start({statusText: "Application is starting..."});

        m_crMessageBus.onMessage = function(event) {
            //m_castLogger.info(LOG_TAG, '[MessageBus] event : ' + JSON.stringify(event) );
            handleMessage(event, LOG_TAG + '.MB');
        }

//        m_crManager.start({statusText: "Application is starting..."});

        m_crManager.setApplicationState("Waiting for match data..."); // displayed as status when selecting device on handheld
    }

    m_crContext.start(crOptions);
}

var m_bDebug     = false;
const LOG_TAG      = 'SB';
var m_castLogger   = null;

const DebugLogger = {
    getInstance : function() {
        return this;
    },
    setEnabled : function(b) {
        console.log('Dummy enabled ' + b);
    },
    showDebugLogs : function(b) {
        console.log('Dummy enabled ' + b);
    },
    warn : function(sTag, sMessage) {
        if ( ! sMessage ) {
            sMessage = sTag;
            sTag = LOG_TAG;
        }
        console.warn('[' + sTag + '] ' + sMessage );
    },
    info : function(sTag, sMessage) {
        if ( ! sMessage ) {
            sMessage = sTag;
            sTag = LOG_TAG;
        }
        console.log('[' + sTag + '] ' + sMessage );
    },
    error : function(sTag, sMessage) {
        if ( ! sMessage ) {
            sMessage = sTag;
            sTag = LOG_TAG;
        }
        console.error('[' + sTag + '] ' + sMessage );
    },
    debug : function(sTag, sMessage) {
        if ( ! sMessage ) {
            sMessage = sTag;
            sTag = LOG_TAG;
        }
        console.debug('[' + sTag + '] ' + sMessage );
    },
}
