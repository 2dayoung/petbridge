/* eslint-disable jsx-a11y/click-events-have-key-events */
/* eslint-disable jsx-a11y/no-static-element-interactions */

const ArticleItem = ({data, onSelectArticle}) => {
  return (
    <div
      className="border-stroke mx-auto my-4 h-[425px] w-72 overflow-hidden rounded-xl border "
      onClick={() => {
        onSelectArticle(data)
      }}
    >
      <img
        src={data.thumbnail}
        alt="imag"
        className="size-[300px] object-contain"
      />
      <div className="p-4">
        <div className="mb-4 flex h-8 items-center justify-around space-x-2.5">
          <img
            src="https://via.placeholder.com/50"
            alt="Author Avatar"
            className="size-12 rounded-full border "
          />
          <div className="flex-1">
            <p className="text-lg font-semibold">{data.nickname}</p>
          </div>

          <img
            src="/icons/icon-comment.svg"
            alt="Comment Icon"
            className="w-5"
          />
          <div>{data.commentCount}</div>
        </div>
        <div className=" text-overflow hover:text-clip">{data.title}</div>
      </div>
    </div>
  )
}
export default ArticleItem
