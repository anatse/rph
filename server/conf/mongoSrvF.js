db.system.js.save ({
    _id: "soundex",
    value: function (word) {
        var rules = [
            [/[aehiouy]/g, ''],
            [/[йуеёыахоэяиюьъ]/g, ''],
            [/[с]?тч/g, 'щ'],
            [/rl/g, 'r'],
            [/[bfpvw]/g, '1'],
            [/[бфпв]/g, '1'],
            [/[cgjkqsxz]/g, '2'],
            [/[цжкзсг]/g, '2'],
            [/[dt]/g, '3'],
            [/[дтщшч]/g, '3'],
            [/[l]/g, '4'],
            [/[л]/g, '4'],
            [/[mn]/g, '5'],
            [/[мн]/g, '5'],
            [/[r]/g, '6'],
            [/[р]/g, '6'],
            [/([0-9])(\1{1,})/g, '$1']
        ]

        var result = word.toLowerCase();
        rules.forEach(function(rule) {
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
        return (
            textData === searchString
            || textData === rusLayout(searchString)
            || textData.indexOf(searchString) > 0
            || soundex(textData) === soundex (searchString)
            || soundex (textData) === soundex (rusLayout(searchString))
        );
    }
});


db.loadServerScripts();