
function highlightTrio() {
    var cards = findTrio();
    if (cards) {
        $(cards)
            .one("webkitAnimationEnd oanimationend msAnimationEnd animationend", function () {
                $(this).removeClass("blink");
            })
            .addClass("blink");
    }
}

function findTrio() {
    var cards = $(".card");
    for (var i = 0; i < cards.length - 2; i++) {
        var c1 = cards[i];
        if (c1 == null)
            continue;
        // --- find trios with card i
        for (var j = i + 1; j < cards.length - 1; j++) {
            var c2 = cards[j];
            if (c2 == null)
                continue;
            // --- find trios with card i+j
            for (var k = j + 1; k < cards.length; k++) {
                var c3 = cards[k];
                if (c3 == null)
                    continue;
                // --- is i+j+k a trio?
                if (isTrio(c1, c2, c3) == null) {
                    return [c1, c2, c3];
                }
            }
        }
    }
    return null;
}

function isTrio(card1, card2, card3) {
    var value1 = parseInt(card1.firstChild.id.substr(5), 10)
    var value2 = parseInt(card2.firstChild.id.substr(5), 10)
    var value3 = parseInt(card3.firstChild.id.substr(5), 10)
    var attributes = [ATTR_NUMBER, ATTR_FILL, ATTR_SHAPE, ATTR_COLOR];
    for (var i=0; i<attributes.length; i++) {
        var attr = attributes[i];
        if (getCardAttribute(value1, attr) == getCardAttribute(value2, attr)) {
            // --- check they are all equal
            if (getCardAttribute(value1, attr) != getCardAttribute(value3, attr)) {
                // --- not a trio
                return attr;
            }
        } else {
            // --- check they are all different
            if (getCardAttribute(value1, attr) == getCardAttribute(value3, attr)) {
                // --- not a trio
                return attr;
            } else if (getCardAttribute(value2, attr) == getCardAttribute(value3, attr)) {
                // --- not a trio
                return attr;
            }
        }
    }
    return null;
}
