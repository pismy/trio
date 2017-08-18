var stompClient = null;
var csrf = null;
var userId = null;
var gameId = null;
var game = null;
var clockId = null;

/**
 * Blocking queue for managing queued animations
 */
var Queue = (function(){

    function Queue() {};

    Queue.prototype.running = false;

    Queue.prototype.queue = [];

    Queue.prototype.add = function(animationFn) {
        var _this = this;
        // add animationFn to the queue
        this.queue.push(function() {
            var finished = animationFn(_this);
            // if animationFn returns void or true, assume animation is over
            // otherwise, next() will have to be called sometime by the animationFn
            if(typeof finished === "undefined" || finished) {
                _this.next();
            }
        });

        if(!this.running) {
            // if nothing is running, then start the engines!
            this.next();
        }

        return this; // for chaining fun!
    }

    Queue.prototype.next = function(){
        this.running = false;
        //get the first element off the queue
        var head = this.queue.shift();
        if(head) {
            this.running = true;
            head();
        }
    }

    return Queue;

})();

var animationQueue = new Queue();

/**
 * Connects to the websocket and subscribes to game engine messages
 */
function connect() {
    console.log("connecting...");
    var socket = new SockJS('/trio-websocket');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, function (frame) {
        console.log('... connected: ' + frame);
        stompClient.subscribe('/down/games/'+gameId, handleEvent);
    });
}

/**
 * Disconnects from the websocket
 */
function disconnect() {
    if (stompClient != null) {
        stompClient.disconnect();
    }
    console.log("disconnected");
}


/**
 * Websocket message handler
 * @param msg message to handle
 */
function handleEvent(msg) {
    var event = JSON.parse(msg.body);
    console.log("<<<", msg.body);
    switch(event.type) {
        case "game_state_changed":
            switch(event.state) {
                case "preparing":
                    message("Let's begin a new game!");
                    break;
                case "playing":
                    message("Game starts!");
                    break;
                case "over":
                    message("Game is over!");
                    break;
                case "finished":
                    message("Game is finished!");
                    break;
            }
            reloadGame(event.state == "playing");
            break;
        case "player_joined":
            message("<b>"+event.player.name+"</b> joined");
            reloadGame();
            break;
        case "player_left":
            message("<b>"+event.player.name+"</b> left");
            reloadGame();
            break;
        case "player_selects":
            message("<b>"+event.player.name+"</b> is selecting a trio...");
            handleGameEvent(event);
            if(userId == event.player.id) {
                startTrioSelection();
            }
            break;
        case "player_declares":
            message("<b>"+event.player.name+"</b> declared a trio");
            handleGameEvent(event);
            break;
        case "select_timeout":
            message("<b>"+event.player.name+"</b> timeouted selecting a trio");
            handleGameEvent(event);
            if(userId == event.player.id) {
                stopTrioSelection("Trio!", "declare_trio");
            }
            break;
        case "select_giveup":
            message("<b>"+event.player.name+"</b> gave up selecting a trio");
            handleGameEvent(event);
            if(userId == event.player.id) {
                stopTrioSelection("Trio!", "declare_trio");
            }
            break;
        case "select_nolonger":
            message("<b>"+event.player.name+"</b> no longer has a trio");
            handleGameEvent(event);
            if(userId == event.player.id) {
                stopTrioSelection("Trio!", "declare_trio");
            }
            break;
        case "select_success":
            message("<b>"+event.player.name+"</b> successfully selected a trio");
            highlightTrio(event);
            handleGameEvent(event);
            if(userId == event.player.id) {
                setAction("Trio!", "declare_trio");
            }
            break;
        case "select_failure":
            message("<b>"+event.player.name+"</b> selected a wrong trio");
            handleGameEvent(event);
            if(userId == event.player.id) {
                setAction("Trio!", "declare_trio");
            }
            break;
        case "cards_drawn":
            drawCards(event);
            break;
        case "cards_moved":
            // message(""+event.from.length+" cards moved");
            moveCards(event);
            break;
    }
}

/**
 * Enters the trio selection mode
 */
function startTrioSelection() {
    // action button: cancel trio selection
    setAction("Cancel", "cancel_trio");
    // toggle board and cards to selectable mode
    $("#board").addClass("select");
    $(".card").click(selectCard);
    // start timer
    var timer = $("#timer");
    timer
        .removeClass("hidden")
        .text(5);
    clockId = setInterval(function() {
        timer
            .text(Number.parseInt(timer.text())-1);
    }, 1000);
}

/**
 * Exists the trio selection mode
 */
function stopTrioSelection(actionName, actionType) {
    // toggle board and cards to unselectable mode
    $("#board").removeClass("select");
    // unselect all cards and
    $(".card").removeClass("selected").off("click");
    // stop timer
    clearInterval(clockId);
    $("#timer").addClass("hidden");
    // set action button
    setAction(actionName, actionType);
}

/**
 * Card selection handler function
 * @param evt browser click event
 */
function selectCard(evt) {
    console.log("select card", evt);
    // retrieve .card element
    var card = $(evt.target);
    if(card.hasClass("symbol")) {
        card = card.parent(".card");
    }
    // toggle selected state
    card.toggleClass("selected");
    // if 3 selected cards: stop selection mode and send selection action
    var selection = $.map($(".card.selected"), function(e) {return Number.parseInt(e.id.substr(5))});
    if(selection.length == 3) {
        stopTrioSelection("Wait", null);
        send({'type': 'select_trio', 'selection': selection});
    }
}

/**
 * Action button click handler
 * @param evt event
 */
function actionClicked(evt) {
    var action = $(evt.target).prop("action");
    if(action) {
        send({'type': action});
    }
}

/**
 * Changes the action button state
 * @param actionName action button title
 * @param actionType action type or null if disabled
 */
function setAction(actionName, actionType) {
    var button = $("#action").text(actionName).prop("action", actionType);
    if(actionType == null) {
        // disable
        button.addClass("hidden");
    } else {
        // enable
        button.removeClass("hidden");
    }
}

/**
 * Displays the given text in the message box
 * @param text text to display
 */
function message(html) {
    // TODO: fade out previous message, fade in new message
    // $("#messages .previous").remove();
    // $("#messages .current").className("previous");
    $("#messages .current").remove();
    $("#messages").append($("<span class='current'>"+html+"</span>"))
}

/**
 * Changes the player state and score
 * @param event the game event to handle
 */
function handleGameEvent(event) {
    // update selection queue
    game.queue = event.queue;

    // update score (if has changed)
    if(event.newScore != null) {
        game.scores[event.player.id] = event.newScore;
        $("#player_"+event.player.id+" .score")
            .text(event.newScore)
            .one("webkitAnimationEnd oanimationend msAnimationEnd animationend", function() {
                $(this).removeClass("highlight");
            })
            .addClass("highlight");
        sortPlayersByScore();
    }

    // update player selection status
    for(var playerId in game.players) {
        var playerElt = $("#player_"+playerId);
        var selectionRank = event.queue.indexOf(playerId);
        if(selectionRank < 0) {
            // looking for trio: no style
            playerElt
                .attr("class", null)
                .find(".status").empty();
        } else if(selectionRank == 0) {
            // selecting trio
            playerElt
                .attr("class", "selecting")
                .find(".status").empty();
        } else { // selectionRank > 0: waiting
            // waiting for selection
            playerElt
                .attr("class", "waiting")
                .find(".status").html("<span class='rank'>"+selectionRank+"</span>");
        }
    }
}

/**
 * Blink selected cards
 * @param event trio selection event
 */
function highlightTrio(event) {
    // blink selected cards
    animationQueue.add(function() {
        console.log("[START] blink trio animation");
        event.positions.forEach(function(pos, index) {
            $("#slot_"+pos)
                .one("webkitAnimationEnd oanimationend msAnimationEnd animationend", function() {
                    $(this).remove();
                    if(index == 0) {
                        console.log("[END] blink trio animation");
                        animationQueue.next();
                    }
                })
                .addClass("blink");
        });
        return false;
    });
    console.log("[QUEUED] blink trio animation");
}

/**
 * Cards draw event implementation
 */
function drawCards(event) {
    var board = $("#board");
    event.positions.forEach(function(pos, index) {
        var value = event.cards[index].value;
        animationQueue.add(function() {
            console.log("[START] "+event.reason+" draw event for card "+pos)
            // just in case...
            $("#slot_"+pos).remove();
            var card = $(createCard(pos, value));
            card
                .one("webkitAnimationEnd oanimationend msAnimationEnd animationend", function() {
                    $(this).removeClass("highlight");
                    console.log("[END] "+event.reason+" draw event for card "+pos)
                    animationQueue.next();
                })
                .addClass("highlight");
            board.append(card);
            return false;
        });
        console.log("[QUEUED] "+event.reason+" draw event for card "+pos)
    });
    // update cards number in deck
    $("#deck").text(event.nbCardsBeforeDraw - event.cards.length);
}

/**
 * Cards moved event implementation
 */
function moveCards(event) {
    event.from.forEach(function(fromPos, index) {
        var toPos = event.to[index];
        animationQueue.add(function() {
            console.log("[START] move card event from "+fromPos+" to "+toPos);
            $("#slot_" + fromPos)
                .one("webkitTransitionEnd otransitionend oTransitionEnd msTransitionEnd transitionend", function() {
                    console.log("[END] move card event from "+fromPos+" to "+toPos);
                    animationQueue.next();
                })
                .attr("id", "slot_" + toPos);
            return false;
        });
        console.log("[QUEUED] move card event from "+fromPos+" to "+toPos);
    });
}

/**
 * Sends an action to the game engine
 * @param action player action
 */
function send(action) {
    console.log(">>>", JSON.stringify(action));
    $.ajax({
        method: "POST",
        url: "/games/"+gameId+"/actions",
        headers: {
            "X-CSRF-TOKEN": csrf
        },
        // The key needs to match your method's input parameter (case-sensitive).
        data: JSON.stringify(action),
        contentType: "application/json; charset=utf-8",
        success: function(){console.log("action succeeded")},
        error: function(xhr, status, error) {
            console.log("action failed: "+xhr.status+" / "+xhr.responseText, error);
        }
    });
}

/**
 * Complete game reload (JSON/REST)
 */
function reloadGame(skipBoard) {
    $.get("/games/"+gameId, function( game, status ) {
        window.game = game;
        console.log( "Game (status "+status+"):", game);
        // --- 1: action button
        switch(game.state) {
            case "preparing":
                if(game.ownerId == userId) {
                    // owner
                    setAction("Start", "start_game");
                } else if(userId != null) {
                    if(game.players[userId] == null) {
                        // not part of the game yet
                        setAction("Join", "player_join");
                    } else {
                        // part of the game
                        setAction("Leave", "player_leave");
                    }
                } else {
                    // anonymous
                    setAction("Signin", null);
                }
                break;
            case "playing":
                if(userId != null && game.players[userId] != null) {
                    var selIdx = game.queue.indexOf(userId);
                    if(selIdx == 0) {
                        // currently queue
                        setAction("Cancel", "cancel_trio");
                    } else if(selIdx > 0) {
                        // in the selection queue
                        $( "#action" ).text("Wait").off("click");
                    } else if(game.players[userId] != null) {
                        // looking for trio
                        setAction("Trio!", "declare_trio");
                    }
                } else {
                    // anonymous or not part of game
                    setAction("Wait", null);
                }

                break;
            case "over":
                if(game.ownerId == userId) {
                    // owner
                    setAction("Finish", "finish_game");
                } else if(userId != null) {
                    setAction("Wait", null);
                } else {
                    // anonymous
                    setAction("Signin", null);
                }
                break;
            case "finished":
                if(game.ownerId == userId) {
                    // owner
                    setAction("Again", "prepare_game");
                } else if(userId != null) {
                    setAction("Wait", null);
                } else {
                    // anonymous
                    setAction("Signin", null);
                }
                break;
        }

        // --- 2: players and scores
        var $scores = $("#scores .players");
        $scores.empty();
        for(var playerId in game.players) {
            var row = document.createElement("tr");
            row.id = "player_"+playerId;
            var status = document.createElement("td");
            status.className = "status";
            var selIdx = game.queue.indexOf(playerId);
            if(selIdx == 0) {
                // currently selecting
                row.className = "selecting";
            } else if(selIdx > 0) {
                // in the selection queue
                row.className = "waiting";
                var rank = document.createElement("span");
                rank.className = "rank";
                rank.innerText = selIdx;
                row.appendChild(rank);
            }

            var name = document.createElement("td");
            name.className = "player";
            name.innerText = game.players[playerId].name;
            var score = document.createElement("td");
            score.className = "score";
            score.innerText = game.scores[playerId] | 0;
            row.appendChild(status);
            row.appendChild(name);
            row.appendChild(score);
            $scores.append(row);
        }
        sortPlayersByScore();

        // --- 3: board
        if(!skipBoard) {
            var board = document.getElementById("board");
            $(board).empty();
            for (var i = 0; i < game.board.length; i++) {
                var card = game.board[i];
                if (card != null) {
                    board.appendChild(createCard(i, card.value));
                }
            }
        }

        // --- 4: deck
        $("#deck").text(game.cardsLeft);
    });
}

function sortPlayersByScore() {
    var $scores = $("#scores .players");
    $scores.find('tr').sort(function(row1, row2) {
        return Number.parseInt($('.score', row2).text()) - Number.parseInt($('.score', row1).text());
    }).appendTo($scores);
}

var ATTR_COLOR = 0;
var ATTR_SHAPE = 2;
var ATTR_FILL = 4;
var ATTR_NUMBER = 6;

function getCardAttribute(value, attr) {
    return (value >> attr) & 0x3;
}

function createCard(position, value) {
    var card = document.createElement("div");
    card.className = "card";
    card.id = "slot_"+position;

    var symbol = document.createElement("div");
    symbol.className = "symbol"
        +" color"+(getCardAttribute(value, ATTR_COLOR) + 1)
        +" shape"+(getCardAttribute(value, ATTR_SHAPE) + 1)
        +" fill"+(getCardAttribute(value, ATTR_FILL) + 1)
        +" number"+(getCardAttribute(value, ATTR_NUMBER) + 1)
    symbol.id = "card_"+value;
    card.appendChild(symbol);

    return card;
}

$(function () {
    window.csrf = $("meta[name='_csrf']").attr("content");
    window.userId = $("meta[name='user_id']").attr("content");
    window.gameId = $("meta[name='game_id']").attr("content");

    $("#action").click(actionClicked);

    reloadGame();
    connect();
});
