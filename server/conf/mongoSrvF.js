db.system.js.save ({
    _id: "soundex",
    value: function (word) {
        var metaphone = [
            [/йо|ио|йе|ие/g, "и"],
            [/о|ы|я/g, "а"],
            [/е|ё|э/g, "и"],
            [/ю/g, "у"],
            [/б/g, "п"],
            [/з/g, "с"],
            [/д/g, "т"],
            [/в/g, "ф"],
            [/г/g, "к"],
            [/тс|дс/g, "ц"],
            [/н{2,}/g, "н"],
            [/с{2,}/g, "с"],
            [/р{2,}/g, "р"],
            [/м{2,}/g, "м"],
            [/[уеаыоияюэ]{1,}$/g, ""]
        ];

        var result = word.toLowerCase();
        metaphone.forEach(function(rule) {
            result = result.replace(rule[0], rule[1])
        });

        return result;
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
        };

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
        var textDataWords = lowerText.split (/[ ,.]+/g);
        if (lowerText === lowerSearchString
            || lowerText === rusSearch
            || lowerText.indexOf(lowerSearchString) >= 0
            || lowerText.indexOf(rusSearch) >= 0)
            return true;
        else {
            var sndSearch = soundex (lowerSearchString);
            var sndRus = soundex (rusSearch);
            var arrayLength = textDataWords.length;

            for (var i = 0; i < arrayLength; i++) {
                var sndWord = soundex (textDataWords[i]);
                if (sndWord === sndSearch || sndWord.indexOf(sndSearch) >= 0
                    || sndWord === sndRus || sndWord.indexOf(sndRus) >= 0)
                    return true;
            }

            return false;
        }
    }
});

db.loadServerScripts();

db.products.find ({$where: "compareString (this.drugsFullName, 'fcgthby')"}).explain("executionStats");
