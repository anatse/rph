db.system.js.save ({
    _id: "soundex",
    value: function (word) {
        var metaphone = [
            [/ЙО|ИО|ЙЕ|ИЕ/g, "И"],
            [/О|Ы|Я/g, "А"],
            [/Е|Ё|Э/g, "И"],
            [/Ю/g, "У"],
            [/Б/g, "П"],
            [/З/g, "С"],
            [/Д/g, "Т"],
            [/В/g, "Ф"],
            [/Г/g, "К"],
            [/ТС|ДС/g, "Ц"],
            [/Н{2,}/g, "Н"],
            [/С{2,}/g, "С"],
            [/Р{2,}/g, "Р"],
            [/М{2,}/g, "М"],
            [/[УЕАЫОИЯЮЭ]{1,}$/g, ""]
        ]

        var result = word.toUpperCase();
        metaphone.forEach(function(rule) {
            result = result.replace(rule[0], rule[1])
        })
        return result
    }
});

db.system.js.save ({
    _id: "rusLayout",
    value: function (str) {
        var replacer = {
            "q": "й", "w": "ц", "e": "у", "r": "к", "t": "е", "y": "н", "u": "г",
            "i": "ш", "o": "щ", "p": "з", "[": "х", "]": "ъ", "a": "ф", "s": "ы",
            "d": "в", "f": "а", "g": "п", "h": "р", "j": "о", "k": "л", "l": "д",
            ";": "ж", "'": "э", "z": "я", "x": "ч", "c": "с", "v": "м", "b": "и",
            "n": "т", "m": "ь", ",": "б", ".": "ю", "/": "."
        }

        return str.replace(new RegExp("[A-z\\/,.;\\'\\]\\[]", "g"), function (x) {
            return x == x.toLowerCase() ? replacer[x] : replacer[x.toLowerCase()].toUpperCase();
        });
    }
});

db.system.js.save({
    _id: "compareString",
    value : function (textData, searchString ) {
        var rusSearch = rusLayout(searchString).toLowerCase();
        var lowerText = textData.toLowerCase();
        var lowerSearchString = searchString.toLowerCase();
        var textDataWords = lowerText.split (new RegExp("[ ,.]+"));

        function isTextIn (words, word) {
            var wordSoundex = soundex (word);
            var arrayLength = words.length;
            for (var i = 0; i < arrayLength; i++) {
                var sndWord = soundex (words[i]);
                if (sndWord === wordSoundex
                    || sndWord.indexOf(wordSoundex) >= 0)
                    return true;
            }

            return false;
        }

        if (lowerText === lowerSearchString
            || lowerText.toLowerCase() === rusSearch
            || lowerText.indexOf(lowerSearchString) >= 0
            || lowerText.indexOf(rusSearch) >= 0)
            return true;
        else {
            var sndText = soundex(lowerText);
            var sndSearch = soundex (lowerSearchString);
            var sndRus = soundex (rusSearch);

            return (
                sndText === sndSearch
                || sndText === sndRus
                || isTextIn(textDataWords, sndSearch)
                || isTextIn(textDataWords, sndRus)
            );
        }
    }
});

db.loadServerScripts();
